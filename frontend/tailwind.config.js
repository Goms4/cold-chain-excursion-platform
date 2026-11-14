/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        accent:  'var(--accent)',
        'accent-hover': 'var(--accent-hover)',
        'accent-tint':  'var(--accent-tint)',
        border:  'var(--border)',
        surface: 'var(--bg-surface)',
        page:    'var(--bg-page)',
        text:    'var(--text)',
        muted:   'var(--text-muted)',
        critical: 'var(--sev-critical)',
        major:   'var(--sev-major)',
        minor:   'var(--sev-minor)',
      },
      fontFamily: {
        sans: ['IBM Plex Mono', 'ui-monospace', 'monospace'],
        mono: ['IBM Plex Mono', 'ui-monospace', 'monospace'],
      },
      borderRadius: {
        DEFAULT: '1px',
      },
    },
  },
  plugins: [],
}
