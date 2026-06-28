import { act, cleanup, render, waitFor } from "@testing-library/react";
import { StrictMode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

const vditorMock = vi.hoisted(() => ({
  construct: vi.fn(),
  destroy: vi.fn(),
  setValue: vi.fn(),
  options: null as null | {
    _lutePath?: string;
    after?: () => void;
    cdn?: string;
    icon?: string;
    i18n?: unknown;
    preview?: {
      hljs?: { enable?: boolean };
      markdown?: {
        codeBlockPreview?: boolean;
        mathBlockPreview?: boolean;
      };
    };
    value?: string;
    input?: (value: string) => void;
  },
}));

vi.mock("vditor", () => ({
  default: class MockVditor {
    private value: string;

    constructor(
      _element: HTMLElement,
      options: {
        _lutePath?: string;
        after?: () => void;
        cdn?: string;
        icon?: string;
        i18n?: unknown;
        preview?: {
          hljs?: { enable?: boolean };
          markdown?: {
            codeBlockPreview?: boolean;
            mathBlockPreview?: boolean;
          };
        };
        value?: string;
        input?: (value: string) => void;
      },
    ) {
      vditorMock.construct();
      this.value = options.value ?? "";
      vditorMock.options = options;
    }

    getValue() {
      return this.value;
    }

    setValue(value: string) {
      this.value = value;
      vditorMock.setValue(value);
    }

    destroy() {
      vditorMock.destroy();
    }
  },
}));

import { VditorMarkdownEditor } from "@/features/notes/editor/VditorMarkdownEditor";

describe("VditorMarkdownEditor", () => {
  afterEach(() => {
    cleanup();
    vditorMock.construct.mockReset();
    vditorMock.destroy.mockReset();
    vditorMock.setValue.mockReset();
    vditorMock.options = null;
  });

  it("creates one editor in Strict Mode and round-trips raw Markdown", async () => {
    const onChange = vi.fn();
    const { rerender, unmount } = render(
      <StrictMode>
        <VditorMarkdownEditor onChange={onChange} value={"# Initial\n\nBody"} />
      </StrictMode>,
    );
    await waitFor(() => expect(vditorMock.construct).toHaveBeenCalledOnce());
    act(() => vditorMock.options?.after?.());

    expect(vditorMock.options?.value).toBe("# Initial\n\nBody");
    expect(vditorMock.options?._lutePath).toBeTruthy();
    expect(vditorMock.options?._lutePath).not.toMatch(/^https?:/);
    expect(vditorMock.options?.cdn).toBe("");
    expect(vditorMock.options?.icon).toBe("ant");
    expect(vditorMock.options?.i18n).toBeTruthy();
    expect(vditorMock.options?.preview).toEqual({
      hljs: { enable: false },
      markdown: { codeBlockPreview: false, mathBlockPreview: false },
    });
    expect(document.getElementById("vditorIconScript")).toHaveAttribute(
      "data-source",
      "bundled",
    );
    act(() => vditorMock.options?.input?.("## Raw\n\n`code`"));
    expect(onChange).toHaveBeenCalledWith("## Raw\n\n`code`");

    rerender(
      <StrictMode>
        <VditorMarkdownEditor onChange={onChange} value={"## Server value"} />
      </StrictMode>,
    );
    await waitFor(() =>
      expect(vditorMock.setValue).toHaveBeenCalledWith("## Server value"),
    );

    unmount();
    expect(vditorMock.destroy).toHaveBeenCalledOnce();
  });

  it("cancels initialization when the component unmounts immediately", async () => {
    const { unmount } = render(
      <VditorMarkdownEditor onChange={vi.fn()} value="" />,
    );

    unmount();
    await act(async () => Promise.resolve());

    expect(vditorMock.construct).not.toHaveBeenCalled();
    expect(vditorMock.destroy).not.toHaveBeenCalled();
  });
});
