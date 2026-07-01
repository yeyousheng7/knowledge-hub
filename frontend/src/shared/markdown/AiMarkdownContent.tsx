import rehypeSanitize, { defaultSchema } from "rehype-sanitize";
import ReactMarkdown, { defaultUrlTransform } from "react-markdown";
import { Database } from "lucide-react";
import { isValidElement, type ReactNode } from "react";
import { Link } from "react-router-dom";
import remarkGfm from "remark-gfm";

import { parseKhSourceHref } from "@/shared/markdown/kh-source";

interface AiMarkdownContentProps {
  content: string;
}

const aiSanitizeSchema = {
  ...defaultSchema,
  protocols: {
    ...defaultSchema.protocols,
    href: [...(defaultSchema.protocols?.href ?? []), "kh-source"],
  },
};

function aiUrlTransform(url: string): string {
  if (url.toLowerCase().startsWith("kh-source:")) {
    return url;
  }

  return defaultUrlTransform(url);
}

function childrenToText(children: ReactNode): string {
  if (typeof children === "string" || typeof children === "number") {
    return String(children);
  }

  if (Array.isArray(children)) {
    return children.map(childrenToText).join("");
  }

  if (isValidElement<{ children?: ReactNode }>(children)) {
    return childrenToText(children.props.children);
  }

  return "";
}

export function AiMarkdownContent({ content }: AiMarkdownContentProps) {
  return (
    <div className="markdown-body ai-markdown">
      <ReactMarkdown
        components={{
          a: ({ href, children, title }) => {
            if (href?.toLowerCase().startsWith("kh-source:")) {
              const target = parseKhSourceHref(href);
              const sourceTitle = title || childrenToText(children) || "知识来源";

              return target ? (
                <Link
                  className="group/source relative mx-1 inline-flex max-w-48 items-center gap-1.5 rounded-full border border-blue-100 bg-blue-50 px-2.5 py-1 align-middle text-xs font-medium leading-none text-blue-700 no-underline shadow-sm shadow-blue-50 transition hover:border-blue-200 hover:bg-blue-600 hover:text-white"
                  title={sourceTitle}
                  to={target.path}
                >
                  <Database aria-hidden="true" className="size-3.5 shrink-0" />
                  <span className="truncate">{sourceTitle}</span>
                  <span
                    aria-hidden="true"
                    className="pointer-events-none absolute left-0 top-full z-20 mt-2 hidden max-w-72 whitespace-nowrap rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-800 opacity-0 shadow-lg shadow-slate-200/70 transition group-hover/source:block group-hover/source:opacity-100"
                  >
                    {sourceTitle}
                  </span>
                </Link>
              ) : (
                <span className="text-slate-600" title="无效的知识来源链接">
                  {children}
                </span>
              );
            }

            const isExternal = /^https?:\/\//i.test(href ?? "");

            return (
              <a
                href={href}
                rel={isExternal ? "noreferrer noopener" : undefined}
                target={isExternal ? "_blank" : undefined}
                title={title}
              >
                {children}
              </a>
            );
          },
        }}
        rehypePlugins={[[rehypeSanitize, aiSanitizeSchema]]}
        remarkPlugins={[remarkGfm]}
        urlTransform={aiUrlTransform}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
