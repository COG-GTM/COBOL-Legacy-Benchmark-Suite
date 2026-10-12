import { useEffect } from 'react';
import { messages } from '../i18n/messages';

export function useDocumentTitle(title: string) {
  useEffect(() => {
    document.title = title === messages.app.name ? title : `${title} – ${messages.app.name}`;
  }, [title]);
}
