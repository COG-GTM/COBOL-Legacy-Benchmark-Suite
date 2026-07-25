const { createApp } = require('./app');

const PORT = process.env.PORT || 4000;
createApp().listen(PORT, () => {
  console.log(`[web-middleware] API listening on http://localhost:${PORT}`);
});
