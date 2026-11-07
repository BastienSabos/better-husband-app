package com.bastiensabos.betterhusband.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.functions
import com.lemonappdev.konsist.api.ext.list.properties
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Les conventions mécaniques d'AGENTS.md, vérifiées sur les sources : un échec cite le fichier et la ligne.
// Le jugement (lisibilité, ton, vie privée) reste aux agents relecteurs.
class ConventionsTest {
    private val files = Konsist.scopeFromProject().files

    private fun sources(path: String) = files.filter { "/$path" in it.path && "/src/main/" in it.path }

    private val domain = sources("core/domain/")
    private val app = sources("app/")

    private val root =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }

    @Test
    fun `le domaine pur ne depend pas d'Android ni de Hilt`() {
        assertTrue("aucune source de :core:domain trouvée : la règle ne vérifierait rien", domain.isNotEmpty())
        domain.assertFalse { file ->
            file.imports.any {
                it.name.startsWith("android.") || it.name.startsWith("androidx.") || it.name.startsWith("dagger.") ||
                    it.name.startsWith("javax.inject.")
            }
        }
    }

    @Test
    fun `la couche donnees ne depend pas de l'interface`() {
        val data = app.filter { "/betterhusband/data/" in it.path }
        assertTrue("aucune source data trouvée", data.isNotEmpty())
        data.assertFalse { file ->
            file.imports.any {
                it.name.startsWith("androidx.compose.") ||
                    it.name.startsWith("com.bastiensabos.betterhusband.feature.") ||
                    it.name.startsWith("com.bastiensabos.betterhusband.ui.")
            }
        }
    }

    @Test
    fun `un ViewModel n'accede pas aux DAO ni a Room`() {
        app.filter { it.name.endsWith("ViewModel") }.assertFalse { file ->
            file.imports.any {
                it.name.startsWith("com.bastiensabos.betterhusband.data.local.") || it.name.startsWith("androidx.room.")
            }
        }
    }

    @Test
    fun `un ViewModel n'expose pas d'etat modifiable`() {
        app
            .flatMap { it.classes() }
            .filter { cls -> cls.hasParentWithName("ViewModel") }
            .flatMap { it.properties() }
            .filter { Regex("""[:=]\s*Mutable""").containsMatchIn(it.text) }
            .assertTrue { it.hasPrivateModifier }
    }

    @Test
    fun `pas de GlobalScope, de runBlocking ni de double bang dans le code de production`() {
        (domain + app).assertFalse { file ->
            Regex("""\bGlobalScope\b|\brunBlocking\b|!!""").containsMatchIn(codeWithoutComments(file.text))
        }
    }

    @Test
    fun `les dispatchers sont injectes, sauf dans le module qui les fournit`() {
        app.filterNot { "/di/" in it.path }.assertFalse { file ->
            Regex("""\bDispatchers\.""").containsMatchIn(codeWithoutComments(file.text))
        }
    }

    @Test
    fun `aucun journal Android ni println, qui risqueraient de contenir des donnees intimes`() {
        (domain + app).assertFalse { file ->
            Regex("""\bLog\.[a-z]\(|\bprintln\(|android\.util\.Log""").containsMatchIn(codeWithoutComments(file.text))
        }
    }

    @Test
    fun `aucune couleur en dur hors du theme`() {
        app.filterNot { "/ui/theme/" in it.path }.assertFalse { it.text.contains("Color(0x") }
    }

    @Test
    fun `aucun texte d'interface en dur`() {
        // ThemePreviews.kt n'est qu'une planche de prévisualisation du thème : ses textes d'exemple ne sont pas affichés.
        app.filterNot { it.path.endsWith("/ui/theme/ThemePreviews.kt") }.assertFalse { file ->
            Regex("""\bText\(\s*(text\s*=\s*)?"[^"]*\p{L}{2}""").containsMatchIn(file.text)
        }
    }

    @Test
    fun `chaque ecran public a un parametre modifier et un apercu`() {
        app
            .functions()
            .filter { it.name.endsWith("Screen") && it.hasAnnotationWithName("Composable") && !it.hasPrivateModifier }
            .assertTrue { screen ->
                screen.hasParameter { it.name == "modifier" } && screen.containingFile.text.contains("@Preview")
            }
    }

    @Test
    fun `les tests du domaine sont nommes par une phrase`() {
        files
            .filter { "/core/domain/src/test/" in it.path }
            .functions()
            .filter { it.hasAnnotationWithName("Test") }
            .assertTrue { " " in it.name }
    }

    private fun manifests() =
        root
            .walkTopDown()
            .filter { it.name == "AndroidManifest.xml" && "/src/main/" in it.path && "/build/" !in it.path }
            .toList()

    @Test
    fun `aucun manifeste ne demande la permission reseau`() {
        val found = manifests()
        assertTrue("aucun manifeste trouvé : la règle ne vérifierait rien", found.isNotEmpty())
        val permission = Regex("""<uses-permission[^>]*android\.permission\.(INTERNET|ACCESS_NETWORK_STATE)""")
        found.forEach { manifest ->
            assertFalse("${manifest.path} demande une permission réseau", permission.containsMatchIn(manifest.readText()))
        }
    }

    @Test
    fun `l'application desactive la sauvegarde Android`() {
        val manifest = manifests().first { "/app/" in it.path }.readText()
        assertTrue("allowBackup doit rester à false", """android:allowBackup="false"""" in manifest)
    }

    @Test
    fun `chaque migration Room a son test`() {
        val declared =
            Regex("""val (MIGRATION_\d+_\d+)\b""")
                .findAll(
                    File(root, "app/src/main/kotlin/com/bastiensabos/betterhusband/data/local/Migrations.kt").readText(),
                ).map { it.groupValues[1] }
                .toList()
        val tests =
            root
                .walkTopDown()
                .first { it.name == "MigrationTest.kt" && "/build/" !in it.path }
                .readText()
        assertTrue("aucune migration trouvée : la règle ne vérifierait rien", declared.isNotEmpty())
        val untested = declared.filterNot { Regex("""\b$it\b""").containsMatchIn(tests) }
        assertTrue("migrations absentes de MigrationTest : $untested", untested.isEmpty())
    }

    /** Sans les commentaires ni la documentation, qui peuvent citer ces mots sans les employer. */
    private fun codeWithoutComments(text: String) =
        text
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .lines()
            .joinToString("\n") { it.substringBefore("//") }
}
