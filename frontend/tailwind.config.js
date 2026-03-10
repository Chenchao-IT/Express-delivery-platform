/** @type {import('tailwindcss').config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#0066DB',
          hover: '#0756B0',
          disabled: '#99C9FF',
          border: '#E0EFFF',
        },
        text: {
          primary: '#171E26',
          secondary: '#586574',
          tertiary: '#818E9C',
          disabled: '#C6CCD2',
        },
        success: '#47B881',
        warning: {
          DEFAULT: '#FFAD0D',
          text: '#D97706',
        },
        danger: '#F64C4C',
        fill: {
          hover: '#F4F5F6',
          disabled: '#FAFAFA',
          border: '#EDEEEF',
        }
      },
      fontFamily: {
        sans: ['Outfit', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', 'sans-serif'],
      },
      boxShadow: {
        'dropdown': '0 4px 10px rgba(0, 0, 0, 0.1)',
        'card': '0 4px 24px rgba(23, 30, 38, 0.08)',
        'pop': '0 4px 12px rgba(23, 30, 38, 0.08)',
      },
      borderRadius: {
        'input': '8px',
        'button': '10px',
        'card': '12px',
      },
      zIndex: {
        dropdown: '10',
        sticky: '20',
        'modal-backdrop': '30',
        modal: '40',
        toast: '50',
      }
    },
  },
  plugins: [],
}
