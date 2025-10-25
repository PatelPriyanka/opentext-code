/** @type {import('tailwindcss').Config} */
module.exports = {
  // CRITICAL: Scan all JavaScript/JSX files for Tailwind classes
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      // Custom OpenText-inspired color palette
      colors: {
        'ot-blue': '#00599c',
        'ot-blue-dark': '#004a80',
        'ot-gray-bg': '#f5f7f8',
        'ot-gray-border': '#e4e7e9',
        'ot-text-dark': '#333',
        'ot-text-light': '#555',
      }
    },
  },
  plugins: [],
}
