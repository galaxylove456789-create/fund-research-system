export function textOrDash(value?: string): string {
  if (value === undefined || value.length === 0) {
    return '--';
  }
  return value;
}

export function numberOrDash(value?: number, digits: number = 2): string {
  if (value === undefined || value === null || value !== value) {
    return '--';
  }
  return value.toFixed(digits);
}

export function percentText(value?: number): string {
  if (value === undefined || value === null || value !== value) {
    return '--';
  }
  const prefix = value > 0 ? '+' : '';
  return prefix + value.toFixed(2) + '%';
}

export function scaleText(value?: number): string {
  if (value === undefined || value === null || value !== value) {
    return '--';
  }
  return value.toFixed(2) + ' 亿';
}

export function rankText(rank?: number, total?: number): string {
  if (rank === undefined || total === undefined || total === 0) {
    return '--';
  }
  return rank.toString() + ' / ' + total.toString();
}

export function shortDate(value?: string): string {
  if (value === undefined || value.length === 0) {
    return '--';
  }
  if (value.length >= 10) {
    return value.substring(0, 10);
  }
  return value;
}
