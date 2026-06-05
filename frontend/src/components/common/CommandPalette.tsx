import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDebounce } from '../../hooks/useDebounce';
import { useLazyGlobalSearchQuery, useExecuteCommandMutation, type SearchResultItem } from '../../services/commandPaletteApi';
import styles from './CommandPalette.module.css';

const RECENT_KEY = 'okabe_cmdk_recent';
const MAX_RECENT = 5;

const TYPE_ICONS: Record<string, string> = {
  workspace: 'W',
  board: 'B',
  card: 'C',
  member: '@',
};

function loadRecent(): SearchResultItem[] {
  try {
    return JSON.parse(localStorage.getItem(RECENT_KEY) ?? '[]');
  } catch { return []; }
}

function saveRecent(items: SearchResultItem[]) {
  localStorage.setItem(RECENT_KEY, JSON.stringify(items.slice(0, MAX_RECENT)));
}

function addRecent(item: SearchResultItem) {
  const recent = loadRecent().filter(r => r.id !== item.id);
  recent.unshift(item);
  saveRecent(recent);
}

export function CommandPalette() {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [activeIndex, setActiveIndex] = useState(0);
  const [isCommand, setIsCommand] = useState(false);
  const [recentItems, setRecentItems] = useState<SearchResultItem[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const debouncedQuery = useDebounce(query, 200);
  const [triggerSearch, { data: searchRes, isFetching }] = useLazyGlobalSearchQuery();
  const [executeCommand] = useExecuteCommandMutation();

  const results = useMemo(() => isCommand
    ? getCommandSuggestions(query)
    : (searchRes?.data ?? []), [isCommand, query, searchRes]);
  useEffect(() => { if (open) setRecentItems(loadRecent()); }, [open]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setOpen(prev => !prev);
      }
      if (e.key === 'Escape' && open) {
        setOpen(false);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [open]);

  useEffect(() => {
    if (open) {
      setQuery('');
      setActiveIndex(0);
      setIsCommand(false);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [open]);

  useEffect(() => {
    if (!open || isCommand) return;
    if (debouncedQuery.trim()) {
      triggerSearch(debouncedQuery.trim());
    }
  }, [debouncedQuery, open, isCommand, triggerSearch]);

  useEffect(() => {
    setActiveIndex(0);
  }, [query, results.length]);

  const handleInputChange = useCallback((value: string) => {
    setQuery(value);
    setIsCommand(value.startsWith('/'));
  }, []);

  const handleSelect = useCallback(async (item: SearchResultItem) => {
    addRecent(item);
    setOpen(false);
    if (item.url) {
      navigate(item.url);
    }
  }, [navigate]);

  const handleCommandExecute = useCallback(async () => {
    try {
      const res = await executeCommand(query).unwrap();
      setOpen(false);
      if (res.data?.type === 'navigate' && res.data?.data) {
        const url = (res.data.data as Record<string, string>).url;
        if (url) navigate(url);
      }
    } catch {
      // command failed silently
    }
  }, [query, executeCommand, navigate]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex(i => Math.min(i + 1, results.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex(i => Math.max(i - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (isCommand) {
        handleCommandExecute();
      } else if (results[activeIndex]) {
        handleSelect(results[activeIndex]);
      }
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  }, [results, activeIndex, isCommand, handleSelect, handleCommandExecute]);

  useEffect(() => {
    if (listRef.current && activeIndex >= 0) {
      const el = listRef.current.children[activeIndex] as HTMLElement;
      el?.scrollIntoView({ block: 'nearest' });
    }
  }, [activeIndex]);

  if (!open) return null;

  return (
    <div className={styles.overlay} onClick={() => setOpen(false)}>
      <div className={styles.modal} onClick={e => e.stopPropagation()}>
        <div className={styles.inputWrapper}>
          <span className={styles.prefix}>{isCommand ? '>' : '🔍'}</span>
          <input
            ref={inputRef}
            className={styles.input}
            placeholder={isCommand ? 'Type a command...' : 'Search boards, cards, members...'}
            value={query}
            onChange={e => handleInputChange(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <kbd className={styles.escHint}>ESC</kbd>
        </div>

        <div className={styles.results} ref={listRef}>
          {!query.trim() && recentItems.length > 0 && (
            <div>
              <div className={styles.sectionLabel}>Recent</div>
              {recentItems.map((item, i) => (
                <div
                  key={item.id}
                  className={`${styles.resultItem} ${i === activeIndex ? styles.active : ''}`}
                  onClick={() => handleSelect(item)}
                  onMouseEnter={() => setActiveIndex(i)}
                >
                  <span className={styles.typeIcon}>{TYPE_ICONS[item.type] ?? '?'}</span>
                  <div className={styles.resultContent}>
                    <span className={styles.title}>{item.title}</span>
                    <span className={styles.breadcrumb}>{item.breadcrumb}</span>
                  </div>
                </div>
              ))}
            </div>
          )}

          {query.trim() && isCommand && (
            <div>
              <div className={styles.sectionLabel}>Commands</div>
              {results.map((cmd, i) => (
                <div
                  key={cmd.id}
                  className={`${styles.resultItem} ${i === activeIndex ? styles.active : ''}`}
                  onClick={handleCommandExecute}
                  onMouseEnter={() => setActiveIndex(i)}
                >
                  <span className={styles.typeIcon}>&gt;</span>
                  <div className={styles.resultContent}>
                    <span className={styles.title}>{cmd.title}</span>
                    <span className={styles.breadcrumb}>{cmd.subtitle}</span>
                  </div>
                </div>
              ))}
            </div>
          )}

          {query.trim() && !isCommand && (
            <div>
              {isFetching && <div className={styles.loading}>Searching...</div>}
              {!isFetching && results.length === 0 && query.trim() && (
                <div className={styles.empty}>No results found</div>
              )}
              {results.map((item, i) => (
                <div
                  key={item.id}
                  className={`${styles.resultItem} ${i === activeIndex ? styles.active : ''}`}
                  onClick={() => handleSelect(item)}
                  onMouseEnter={() => setActiveIndex(i)}
                >
                  <span className={styles.typeIcon}>{TYPE_ICONS[item.type] ?? '?'}</span>
                  <div className={styles.resultContent}>
                    <span className={styles.title}>{item.title}</span>
                    <span className={styles.breadcrumb}>{item.breadcrumb}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className={styles.footer}>
          <span><kbd>↑↓</kbd> Navigate</span>
          <span><kbd>↵</kbd> Open</span>
          <span><kbd>ESC</kbd> Close</span>
        </div>
      </div>
    </div>
  );
}

const COMMANDS = [
  { id: 'cmd_tao', type: 'command' as const, title: '/tao card "title"', subtitle: 'Create a new card', breadcrumb: 'Command', url: null, icon: '>' },
  { id: 'cmd_di', type: 'command' as const, title: '/di board "name"', subtitle: 'Navigate to a board', breadcrumb: 'Command', url: null, icon: '>' },
  { id: 'cmd_move', type: 'command' as const, title: '/move card #id to "list"', subtitle: 'Move card to another list', breadcrumb: 'Command', url: null, icon: '>' },
  { id: 'cmd_search', type: 'command' as const, title: '/search "keyword"', subtitle: 'Search across workspace', breadcrumb: 'Command', url: null, icon: '>' },
];

function getCommandSuggestions(query: string): SearchResultItem[] {
  const q = query.toLowerCase();
  return COMMANDS.filter(c => c.title.toLowerCase().includes(q));
}
