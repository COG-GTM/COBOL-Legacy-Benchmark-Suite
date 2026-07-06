import { createBrowserRouter } from 'react-router-dom';
import { Layout } from './components/Layout';
import { MenuView } from './views/MenuView';
import { PositionInquiryView } from './views/PositionInquiryView';
import { TransactionHistoryView } from './views/TransactionHistoryView';
import { NotFoundView } from './views/NotFoundView';

/**
 * Routes mirror the CLBS BMS maps (INQSET.bms):
 *   /              -> MENMAP  (main menu / INQONLN P200-DISPLAY-MENU)
 *   /positions     -> POSMAP  (INQPORT portfolio position inquiry)
 *   /transactions  -> HISMAP  (INQHIST transaction history)
 */
export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <MenuView /> },
      { path: 'positions', element: <PositionInquiryView /> },
      { path: 'transactions', element: <TransactionHistoryView /> },
      { path: '*', element: <NotFoundView /> },
    ],
  },
]);
