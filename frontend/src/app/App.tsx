import { BookOpenText } from "lucide-react";

export function App() {
  return (
    <main className="grid min-h-svh place-items-center bg-background px-6 text-foreground">
      <section className="w-full max-w-lg rounded-2xl border bg-white p-10 text-center shadow-sm">
        <div className="mx-auto mb-6 grid size-12 place-items-center rounded-xl bg-primary text-primary-foreground">
          <BookOpenText aria-hidden="true" className="size-6" strokeWidth={1.8} />
        </div>
        <p className="text-sm font-medium tracking-wide text-muted-foreground">
          KnowledgeHub
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">
          Frontend foundation is ready.
        </h1>
        <p className="mt-4 text-sm leading-6 text-muted-foreground">
          The application shell and product features will be added in later phases.
        </p>
      </section>
    </main>
  );
}
