import { RxStompConfig } from '@stomp/rx-stomp';

export function myRxStompConfig(token: string): RxStompConfig {
  return {
    brokerURL: 'ws://localhost:8080/ws-chat',

    connectHeaders: {
      Authorization: `Bearer ${token}`
    },

    heartbeatIncoming: 0,
    heartbeatOutgoing: 20000,
    reconnectDelay: 200,

    debug: (msg: string): void => {
      console.log(new Date(), msg);
    }
  };
}
