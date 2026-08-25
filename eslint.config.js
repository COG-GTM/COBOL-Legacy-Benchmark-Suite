const globals = require('globals');

module.exports = [
  {
    files: ['modernized/**/*.js', 'golden/parity/**/*.js'],
    ignores: ['modernized/coverage/**'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'commonjs',
      globals: {
        ...globals.node,
        ...globals.jest,
      },
    },
    rules: {
      'no-console': 'off',
      'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'no-constant-condition': 'off',
      'no-prototype-builtins': 'off',
    },
  },
];
