import { createApp } from "./app";

const port = Number(process.env.PORT ?? 4000);
const app = createApp();

app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`CLBS Portfolio API listening on http://localhost:${port}`);
  // eslint-disable-next-line no-console
  console.log(`Data source: ${process.env.DATA_SOURCE ?? "memory"}`);
});
