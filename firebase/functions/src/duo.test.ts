import { describe, expect, it } from "vitest";
import { idleCutoffMillis, isMember, otherMember, syncMessage, tokenOf } from "./duo";

describe("otherMember", () => {
  it("gives the other phone of the duo", () => {
    expect(otherMember(["alex", "camille"], "alex")).toBe("camille");
    expect(otherMember(["alex", "camille"], "camille")).toBe("alex");
  });

  it("gives nothing while the duo has one member or when the author is a stranger", () => {
    expect(otherMember(["alex"], "alex")).toBeNull();
    expect(otherMember(["alex", "camille"], "zoe")).toBeNull();
    expect(otherMember([], "alex")).toBeNull();
  });
});

describe("isMember", () => {
  it("accepts members only", () => {
    expect(isMember(["alex", "camille"], "camille")).toBe(true);
    expect(isMember(["alex", "camille"], "zoe")).toBe(false);
    expect(isMember(["alex"], undefined)).toBe(false);
  });
});

describe("idleCutoffMillis", () => {
  it("is twelve months before now", () => {
    const now = Date.UTC(2027, 5, 1);
    expect(now - idleCutoffMillis(now)).toBe(365 * 24 * 3_600_000);
  });
});

describe("syncMessage", () => {
  it("carries no visible notification and no content", () => {
    const message = syncMessage("tok");
    expect(message.token).toBe("tok");
    expect(message.data).toEqual({ type: "sync" });
    expect(Object.keys(message)).not.toContain("notification");
    expect(message.android.priority).toBe("high");
  });
});

describe("tokenOf", () => {
  it("reads a non-empty string token", () => {
    expect(tokenOf({ fcmToken: "abc" })).toBe("abc");
    expect(tokenOf({ fcmToken: "" })).toBeNull();
    expect(tokenOf({ fcmToken: 12 })).toBeNull();
    expect(tokenOf(undefined)).toBeNull();
  });
});
