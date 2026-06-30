import rehypeSanitize from "rehype-sanitize";
import ReactMarkdown, { defaultUrlTransform } from "react-markdown";
import remarkGfm from "remark-gfm";

interface MarkdownContentProps {
  content: string;
}

export function MarkdownContent({ content }: MarkdownContentProps) {
  return (
    <div className="markdown-body">
      <ReactMarkdown
        components={{
          a: ({ href, children, title }) => {
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
        rehypePlugins={[rehypeSanitize]}
        remarkPlugins={[remarkGfm]}
        urlTransform={defaultUrlTransform}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
