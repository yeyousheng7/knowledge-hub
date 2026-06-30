import { Eye, EyeOff, LockKeyhole, type LucideIcon } from "lucide-react";
import { useId, useState, type InputHTMLAttributes } from "react";

interface AuthFormFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, "className"> {
  icon: LucideIcon;
  label: string;
}

export function AuthFormField({
  icon: Icon,
  label,
  ...inputProps
}: AuthFormFieldProps) {
  const generatedId = useId();
  const fieldId = inputProps.id ?? generatedId;

  return (
    <div className="relative">
      <label className="sr-only" htmlFor={fieldId}>
        {label}
      </label>
      <Icon
        aria-hidden="true"
        className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400"
        strokeWidth={1.8}
      />
      <input
        {...inputProps}
        className="h-11 w-full rounded-lg border border-slate-200 bg-white/90 pl-10 pr-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:border-blue-500 focus:ring-4 focus:ring-blue-100/70"
        id={fieldId}
      />
    </div>
  );
}

type PasswordFieldProps = Omit<AuthFormFieldProps, "icon" | "type">;

export function PasswordField({ label, ...inputProps }: PasswordFieldProps) {
  const generatedId = useId();
  const fieldId = inputProps.id ?? generatedId;
  const [isVisible, setIsVisible] = useState(false);

  return (
    <div className="relative">
      <label className="sr-only" htmlFor={fieldId}>
        {label}
      </label>
      <LockKeyhole
        aria-hidden="true"
        className="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-slate-400"
        strokeWidth={1.8}
      />
      <input
        {...inputProps}
        className="h-11 w-full rounded-lg border border-slate-200 bg-white/90 pl-10 pr-11 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 hover:border-slate-300 focus:border-blue-500 focus:ring-4 focus:ring-blue-100/70"
        id={fieldId}
        type={isVisible ? "text" : "password"}
      />
      <button
        aria-label={isVisible ? `隐藏${label}` : `显示${label}`}
        className="absolute right-2 top-1/2 grid size-8 -translate-y-1/2 place-items-center rounded-md text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-400"
        onClick={() => setIsVisible((current) => !current)}
        type="button"
      >
        {isVisible ? (
          <EyeOff aria-hidden="true" className="size-4" />
        ) : (
          <Eye aria-hidden="true" className="size-4" />
        )}
      </button>
    </div>
  );
}
