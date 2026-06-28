import Vditor from "vditor";
import "vditor/dist/css/content-theme/light.css";
import "vditor/dist/index.css";
import "vditor/dist/js/i18n/zh_CN.js";
import "vditor/dist/js/icons/ant.js";
import lutePath from "vditor/dist/js/lute/lute.min.js?url";
import { useEffect, useRef } from "react";

import { cn } from "@/shared/lib/utils";

interface VditorMarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  onSaveShortcut?: () => void;
  hideToolbar?: boolean;
}

const TOOLBAR = [
  "headings",
  "bold",
  "italic",
  "strike",
  "link",
  "|",
  "list",
  "ordered-list",
  "check",
  "quote",
  "code",
  "inline-code",
  "|",
  "undo",
  "redo",
  "|",
  "fullscreen",
];

type VditorOptions = NonNullable<ConstructorParameters<typeof Vditor>[1]>;
type VditorI18n = NonNullable<VditorOptions["i18n"]>;
type VditorOptionsWithLutePath = VditorOptions & { _lutePath: string };

const zhCnI18n = (
  window as Window & { VditorI18n: VditorI18n }
).VditorI18n;

function markBundledIconsReady() {
  if (document.getElementById("vditorIconScript")) {
    return;
  }

  const marker = document.createElement("script");
  marker.id = "vditorIconScript";
  marker.dataset.source = "bundled";
  document.head.appendChild(marker);
}

export function VditorMarkdownEditor({
  value,
  onChange,
  onSaveShortcut,
  hideToolbar = false,
}: VditorMarkdownEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<Vditor | null>(null);
  const valueRef = useRef(value);
  const hideToolbarRef = useRef(hideToolbar);
  const onChangeRef = useRef(onChange);
  const onSaveShortcutRef = useRef(onSaveShortcut);

  useEffect(() => {
    valueRef.current = value;
    onChangeRef.current = onChange;
    onSaveShortcutRef.current = onSaveShortcut;
  }, [onChange, onSaveShortcut, value]);

  useEffect(() => {
    const container = containerRef.current;

    if (!container) {
      return undefined;
    }

    let editor: Vditor | null = null;
    let isReady = false;
    let isDisposed = false;

    const destroyEditor = () => {
      if (!editor) {
        return;
      }

      editor.destroy();
      if (editorRef.current === editor) {
        editorRef.current = null;
      }
    };

    queueMicrotask(() => {
      if (isDisposed) {
        return;
      }

      markBundledIconsReady();

      const options: VditorOptionsWithLutePath = {
        value: valueRef.current,
        _lutePath: lutePath,
        cdn: "",
        i18n: zhCnI18n,
        icon: "ant",
        mode: "ir",
        lang: "zh_CN",
        height: "100%",
        minHeight: 360,
        placeholder: "使用 Markdown 记录正文内容…",
        toolbar: hideToolbarRef.current ? [] : TOOLBAR,
        toolbarConfig: {
          hide: hideToolbarRef.current,
          pin: !hideToolbarRef.current,
        },
        cache: { enable: false },
        counter: {
          enable: !hideToolbarRef.current,
          max: 100_000,
          type: "markdown",
        },
        preview: {
          hljs: { enable: false },
          markdown: { codeBlockPreview: false, mathBlockPreview: false },
        },
        input: (nextValue) => onChangeRef.current(nextValue),
        ctrlEnter: () => onSaveShortcutRef.current?.(),
        after: () => {
          isReady = true;

          if (isDisposed) {
            destroyEditor();
            return;
          }

          editorRef.current = editor;
          if (editor && editor.getValue() !== valueRef.current) {
            editor.setValue(valueRef.current, true);
          }
        },
      };
      editor = new Vditor(container, options);
    });

    return () => {
      isDisposed = true;
      if (isReady) {
        destroyEditor();
      }
    };
    // The editor instance is intentionally created once for this form.
    // External value changes are synchronized by the effect below.
  }, []);

  useEffect(() => {
    const editor = editorRef.current;

    if (editor && editor.getValue() !== value) {
      editor.setValue(value, true);
    }
  }, [value]);

  return (
    <div
      aria-label="Markdown 正文"
      className={cn(
        "note-vditor min-h-[360px] overflow-hidden rounded-xl border border-slate-200 bg-white",
        hideToolbar && "note-vditor--plain",
      )}
      ref={containerRef}
    />
  );
}
