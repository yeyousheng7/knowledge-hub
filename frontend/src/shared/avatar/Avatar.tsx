import { getAvatarIdentity } from "@/shared/avatar/avatar-utils";
import { cn } from "@/shared/lib/utils";

interface AvatarProps {
  nickname: string;
  username: string;
  className?: string;
}

export function Avatar({ nickname, username, className }: AvatarProps) {
  const identity = getAvatarIdentity(nickname, username);

  return (
    <span
      aria-label={`${identity.name} 的头像`}
      className={cn(
        "inline-grid size-9 shrink-0 place-items-center rounded-full text-sm font-semibold text-white",
        identity.colorClassName,
        className,
      )}
      role="img"
      title={identity.name}
    >
      {identity.label}
    </span>
  );
}
