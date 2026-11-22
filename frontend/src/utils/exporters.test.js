import { escapeCsvValue, escapeHtmlCell, escapeXml } from "./exporters";

describe("exporters security and sanitization helpers", () => {
  test("escapeCsvValue neutralizes spreadsheet formulas while preserving plain signed numbers and CSV quoting", () => {
    expect(escapeCsvValue("=SUM(A1:A2)")).toBe("'=SUM(A1:A2)");
    expect(escapeCsvValue("+cmd|' /C calc'!A0")).toBe("'+cmd|' /C calc'!A0");
    expect(escapeCsvValue("-2+3")).toBe("'-2+3");
    expect(escapeCsvValue("@SUM(1,1)")).toBe("\"'@SUM(1,1)\"");
    expect(escapeCsvValue("\tcmd")).toBe("'\tcmd");
    expect(escapeCsvValue("=1,\"2\"")).toBe("\"'=1,\"\"2\"\"\"");

    expect(escapeCsvValue("-12.50")).toBe("-12.50");
    expect(escapeCsvValue("+100")).toBe("+100");
    expect(escapeCsvValue("hello, \"world\"\rnext")).toBe("\"hello, \"\"world\"\" next\"");
    expect(escapeCsvValue(null)).toBe("");
  });

  test("escapeHtmlCell escapes HTML metacharacters, neutralizes formulas, and strips control characters", () => {
    expect(escapeHtmlCell("<script>alert(\"x&y\")</script>")).toBe(
      "&lt;script&gt;alert(&quot;x&amp;y&quot;)&lt;/script&gt;"
    );
    expect(escapeHtmlCell("=1+1")).toBe("&#39;=1+1");
    expect(escapeHtmlCell("\rcmd")).toBe("&#39;\rcmd");
    expect(escapeHtmlCell("\tcmd")).toBe("&#39;\tcmd");
    expect(escapeHtmlCell("a\u0000b\u0007c")).toBe("abc");
    expect(escapeHtmlCell("-12.50")).toBe("-12.50");
  });

  test("escapeXml strips illegal XML 1.0 control chars while keeping tabs, newlines, and entity escaping", () => {
    expect(escapeXml("x\u0000\u0008\u000B\u000C\u001Fy")).toBe("xy");
    expect(escapeXml("a\tb\nc\rd")).toBe("a\tb\nc\rd");
    expect(escapeXml("<tag attr=\"v\" alt='s'>&</tag>")).toBe(
      "&lt;tag attr=&quot;v&quot; alt=&apos;s&apos;&gt;&amp;&lt;/tag&gt;"
    );
  });
});
