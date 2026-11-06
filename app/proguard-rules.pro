# Règles R8 propres à l'app. Les bibliothèques AndroidX, Hilt et Room fournissent leurs propres règles.
# Ajouter ici uniquement ce qui est justifié par un crash R8 reproduit, avec un commentaire.

# SQLCipher charge ses classes JNI par réflexion : sans cette règle, R8 les renomme et l'ouverture de la base
# plante en release.
-keep class net.zetetic.database.** { *; }
-keepclassmembers class net.zetetic.database.** { native <methods>; }
