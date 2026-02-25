/**
 * ErrorBanner — inline error message display component.
 * Used across pages for showing transient error messages.
 */

interface Props {
  message: string;
}

export default function ErrorBanner({ message }: Props) {
  if (!message) return null;

  return (
    <div style={styles.banner}>
      {message}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  banner: {
    padding: "10px 16px",
    borderRadius: 4,
    background: "#1E293B",
    border: "1px solid #553333",
    color: "#F87171",
    fontSize: 14,
    marginBottom: 16,
  },
};
