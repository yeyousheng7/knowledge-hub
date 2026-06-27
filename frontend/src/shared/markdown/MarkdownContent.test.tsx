import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import { MarkdownContent } from "@/shared/markdown/MarkdownContent";

describe("MarkdownContent", () => {
  afterEach(cleanup);

  it("renders GFM while blocking raw HTML and unsafe protocols", () => {
    const { container } = render(
      <MarkdownContent
        content={[
          "**safe text**",
          "",
          "- [x] checked",
          "",
          "[unsafe](javascript:alert('xss'))",
          "",
          "<script>alert('xss')</script>",
        ].join("\n")}
      />,
    );

    expect(screen.getByText("safe text").tagName).toBe("STRONG");
    expect(screen.getByRole("checkbox")).toBeChecked();
    expect(screen.getByText("unsafe")).not.toHaveAttribute("href");
    expect(container.querySelector("script")).toBeNull();
  });

  it("protects external links opened in a new tab", () => {
    render(<MarkdownContent content="[OpenAI](https://openai.com)" />);

    expect(screen.getByRole("link", { name: "OpenAI" })).toMatchObject({
      target: "_blank",
      rel: "noreferrer noopener",
    });
  });
});
