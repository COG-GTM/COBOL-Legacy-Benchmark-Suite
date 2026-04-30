import { message } from 'antd';

/**
 * Toast/notification utility for global error handling and success messages
 * Mirrors ERRMAP from src/maps/INQSET.bms lines 89-99
 */

export const toast = {
  success: (content: string) => {
    message.success(content);
  },
  error: (content: string) => {
    message.error(content);
  },
  warning: (content: string) => {
    message.warning(content);
  },
  info: (content: string) => {
    message.info(content);
  },
};

export default toast;
