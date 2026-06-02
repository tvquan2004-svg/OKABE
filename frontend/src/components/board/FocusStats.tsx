import { useGetFocusStatsQuery } from '../../services/focusApi';
import styles from './FocusStats.module.css';

interface FocusStatsProps {
  from?: string;
  to?: string;
}

function FocusStats({ from, to }: FocusStatsProps) {
  const { data: stats, isLoading } = useGetFocusStatsQuery({ from, to });

  if (isLoading) {
    return <div className={styles.loading}>Đang tải...</div>;
  }

  if (!stats) {
    return <div className={styles.empty}>Chưa có dữ liệu focus.</div>;
  }

  const maxMinutes = Math.max(...stats.dailyBreakdown.map((d) => d.minutes), 1);

  const weekChangeLabel =
    stats.weekChangePercent > 0
      ? `${stats.weekChangePercent}% nhiều hơn tuần trước`
      : stats.weekChangePercent < 0
      ? `${Math.abs(stats.weekChangePercent)}% ít hơn tuần trước`
      : 'Tương đương tuần trước';

  const formatDate = (dateStr: string) => {
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('vi-VN', { weekday: 'short' });
  };

  return (
    <div className={styles.container}>
      <div className={styles.summary}>
        <div className={styles.summaryCard}>
          <span className={styles.summaryValue}>{Math.round(stats.todayMinutes / 60 * 10) / 10}h</span>
          <span className={styles.summaryLabel}>Hôm nay</span>
        </div>
        <div className={styles.summaryCard}>
          <span className={styles.summaryValue}>{Math.round(stats.weekMinutes / 60 * 10) / 10}h</span>
          <span className={styles.summaryLabel}>Tuần này</span>
        </div>
        <div className={styles.summaryCard}>
          <span className={styles.summaryValue}>{Math.round(stats.monthMinutes / 60 * 10) / 10}h</span>
          <span className={styles.summaryLabel}>Tháng này</span>
        </div>
      </div>

      <div className={styles.changeBadge}>
        {stats.weekChangePercent > 0 ? '📈' : stats.weekChangePercent < 0 ? '📉' : '➡️'}{' '}
        {weekChangeLabel}
      </div>

      <div className={styles.chartSection}>
        <h4 className={styles.sectionTitle}>Focus theo ngày</h4>
        <div className={styles.chart}>
          {stats.dailyBreakdown.map((day) => (
            <div key={day.date} className={styles.barCol}>
              <div className={styles.barWrapper}>
                <div
                  className={styles.bar}
                  style={{ height: `${(day.minutes / maxMinutes) * 100}%` }}
                >
                  {day.minutes > 0 && (
                    <span className={styles.barLabel}>{Math.round(day.minutes / 60 * 10) / 10}h</span>
                  )}
                </div>
              </div>
              <span className={styles.barDate}>{formatDate(day.date)}</span>
            </div>
          ))}
        </div>
      </div>

      {stats.topCards.length > 0 && (
        <div className={styles.topCardsSection}>
          <h4 className={styles.sectionTitle}>Top cards được focus nhiều nhất</h4>
          <div className={styles.topCardsList}>
            {stats.topCards.map((card, i) => (
              <div key={card.cardId} className={styles.topCardRow}>
                <span className={styles.topCardRank}>{i + 1}</span>
                <div className={styles.topCardInfo}>
                  <span className={styles.topCardTitle}>{card.cardTitle}</span>
                  <span className={styles.topCardMeta}>
                    {card.sessions} phiên · {Math.round(card.totalMinutes / 60 * 10) / 10}h
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default FocusStats;
