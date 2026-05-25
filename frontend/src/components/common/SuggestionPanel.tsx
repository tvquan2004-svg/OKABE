import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useGetSuggestionsQuery,
  useDismissSuggestionMutation,
  type Suggestion,
} from '../../services/suggestionApi';
import styles from './SuggestionPanel.module.css';

interface SuggestionPanelProps {
  workspaceId: number | null;
}

const TYPE_ICONS: Record<string, string> = {
  STALE: '⏰',
  DUE_SOON: '🔥',
  INCOMPLETE: '📝',
  OVERLOADED: '⚠️',
};

const SuggestionPanel: React.FC<SuggestionPanelProps> = ({ workspaceId }) => {
  const navigate = useNavigate();
  const { data: suggestionsRes } = useGetSuggestionsQuery(workspaceId!, {
    skip: !workspaceId,
    pollingInterval: 300000,
  });
  const [dismissSuggestion] = useDismissSuggestionMutation();

  const suggestions = suggestionsRes?.data ?? [];

  if (!workspaceId || suggestions.length === 0) return null;

  const handleView = (s: Suggestion) => {
    navigate(s.actionUrl);
  };

  const handleDismiss = (s: Suggestion) => {
    dismissSuggestion({ type: s.type, cardId: s.cardId, workspaceId });
  };

  return (
    <div className={styles.panel}>
      <div className={styles.header}>
        <span>💡</span>
        <span className={styles.title}>Gợi ý</span>
        <span className={styles.count}>{suggestions.length}</span>
      </div>
      {suggestions.map((s) => (
        <div key={s.id} className={styles.item} onClick={() => handleView(s)}>
          <span className={styles.icon}>{TYPE_ICONS[s.type] ?? '💡'}</span>
          <span className={styles.message}>{s.message}</span>
          <button
            className={styles.dismiss}
            onClick={(e) => { e.stopPropagation(); handleDismiss(s); }}
            title="Bỏ qua"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  );
};

export default SuggestionPanel;
