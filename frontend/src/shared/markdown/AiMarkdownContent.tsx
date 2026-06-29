import rehypeSanitize, { defaultSchema } from "rehype-sanitize";
import ReactMarkdown, { defaultUrlTransform } from "react-markdown";
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

export function AiMarkdownContent({ content }: AiMarkdownContentProps) {
  return (
    <div className="markdown-body ai-markdown">
      <ReactMarkdown
        components={{
          a: ({ href, children, title }) => {
            if (href?.toLowerCase().startsWith("kh-source:")) {
              const target = parseKhSourceHref(href);

              return target ? (
                <Link title={title} to={target.path}>
                  {children}
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
