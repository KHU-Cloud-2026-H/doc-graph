/**
 * 입력된 시각을 현재 시각과 비교하여 자연어 상대 시간 문자열로 변환한다.
 * 입력값의 범위에 따라 자동으로 단위(분/시간/일/개월/년)를 선택한다.
 *
 * @param date Date 객체, ISO 문자열, 또는 epoch milliseconds 숫자
 * @returns "방금 전" | "N분 전" | "N시간 전" | "N일 전" | "N개월 전" | "N년 전"
 *
 * @example
 *   formatRelativeTime(new Date(Date.now() - 2 * 60 * 1000));  // "2분 전"
 *   formatRelativeTime('2026-05-21T13:30:00+09:00');           // "1시간 전" (현재가 14:30이라면)
 */
export function formatRelativeTime(date: Date | string | number): string {
  const target = date instanceof Date ? date : new Date(date);
  const diffMs = Date.now() - target.getTime();
  const diffSec = Math.floor(diffMs / 1000);

  if (diffSec < 60) return '방금 전';

  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}분 전`;

  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;

  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 30) return `${diffDay}일 전`;

  const diffMonth = Math.floor(diffDay / 30);
  if (diffMonth < 12) return `${diffMonth}개월 전`;

  const diffYear = Math.floor(diffMonth / 12);
  return `${diffYear}년 전`;
}
