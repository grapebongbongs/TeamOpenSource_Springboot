self.addEventListener('push', e=>{
  const data = e.data?.json() || {};
  e.waitUntil(self.registration.showNotification(data.title||'오늘의 문제', {
    body: data.body || '2문제가 도착했어요!', data:{ url: data.url || '/question/list' }
  }));
});
self.addEventListener('notificationclick', e=>{
  e.notification.close(); const url = e.notification?.data?.url || '/'; e.waitUntil(clients.openWindow(url));
});
