/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        pharma: {
          50: '#F0FDF4',
          100: '#DCFCE7',
          600: '#15803D',
          700: '#0F4C5C', // Deep pharmaceutical teal/green accent
          800: '#0E3B43',
          900: '#09252B',
        },
        clinical: {
          bg: '#F8FAFC',
          card: '#FFFFFF',
          border: '#E2E8F0',
          text: '#0F172A',
          muted: '#64748B'
        }
      }
    },
  },
  plugins: [],
}
