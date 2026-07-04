interface AppThemeTokens {
  pageBg: string;
  cardBg: string;
  ink: string;
  muted: string;
  subtle: string;
  line: string;
  primary: string;
  primarySoft: string;
  accent: string;
  accentSoft: string;
  warn: string;
  warnSoft: string;
  danger: string;
  dangerSoft: string;
  radius: number;
}

export const AppTheme: AppThemeTokens = {
  pageBg: '#F3F6F8',
  cardBg: '#FFFFFF',
  ink: '#17212B',
  muted: '#687684',
  subtle: '#9AA8B6',
  line: '#E4EAF0',
  primary: '#1565D8',
  primarySoft: '#E8F1FF',
  accent: '#00A783',
  accentSoft: '#E4F8F3',
  warn: '#D97706',
  warnSoft: '#FFF4DF',
  danger: '#C2413F',
  dangerSoft: '#FCE9E8',
  radius: 8
};

export function riskColor(riskLevel?: string): string {
  if (riskLevel === undefined) {
    return AppTheme.muted;
  }
  if (riskLevel.indexOf('高') >= 0) {
    return AppTheme.danger;
  }
  if (riskLevel.indexOf('低') >= 0) {
    return AppTheme.accent;
  }
  return AppTheme.warn;
}

export function riskBg(riskLevel?: string): string {
  if (riskLevel === undefined) {
    return '#EEF2F6';
  }
  if (riskLevel.indexOf('高') >= 0) {
    return AppTheme.dangerSoft;
  }
  if (riskLevel.indexOf('低') >= 0) {
    return AppTheme.accentSoft;
  }
  return AppTheme.warnSoft;
}

export function scoreColor(score?: number): string {
  if (score === undefined) {
    return AppTheme.muted;
  }
  if (score >= 85) {
    return AppTheme.accent;
  }
  if (score >= 75) {
    return AppTheme.primary;
  }
  return AppTheme.warn;
}
