import { PageState } from "@/shared/layout/PageState";

interface PlaceholderPageProps {
  title: string;
  description: string;
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <PageState
      description={description}
      mode="empty"
      title={title}
    />
  );
}
