async function registerSW(){ if(!('serviceWorker'in navigator)) return null; return navigator.serviceWorker.register('/sw.js'); }
function b64ToU8(b64){const p='='.repeat((4-b64.length%4)%4);const a=(b64+p).replace(/-/g,'+').replace(/_/g,'/');const r=atob(a),u=new Uint8Array(r.length);for(let i=0;i<r.length;i++)u[i]=r.charCodeAt(i);return u;}
async function subscribePush(){
  if(!('Notification'in window)) { alert('브라우저가 알림을 지원하지 않습니다.'); return; }
  const perm = await Notification.requestPermission();
  if(perm!=='granted'){ alert('알림 허용이 필요합니다.'); return; }
  const reg = await registerSW();
  const pub = window.VAPID_PUBLIC || ''; // 추후 서버에서 주입
  if(!pub){ alert('VAPID 공개키가 설정되지 않았습니다.'); return; }
  const sub = await reg.pushManager.subscribe({ userVisibleOnly:true, applicationServerKey: b64ToU8(pub) });
  console.log('Subscribed:', sub);
}

async function scheduleNotificationsNow(){
  try{
    await fetch('/api/notifications/schedule-now', { credentials:'same-origin' });
  }catch(e){
    // ignore errors silently
  }
}

// 알림 벨 UI 상태
const notificationCenter = {
  items: [],
  seen: new Set(),
  bell: null,
  panel: null,
  list: null,
  badge: null,
  friendBadge: null
};

function renderNotificationCenter(){
  if(!notificationCenter.list) return;
  const { items, list, badge } = notificationCenter;
  list.innerHTML = '';
  if(!items.length){
    const empty = document.createElement('div');
    empty.className = 'notification-empty';
    empty.innerHTML = '알람이 없습니다.<br>모든 문제를 풀었어요.';
    list.appendChild(empty);
  } else {
    items.forEach(n=>{
      const link = document.createElement('a');
      link.className = 'notification-item';
      link.href = n.url || '/quiz/list';
      link.innerHTML = `
        <div class="notification-title">${n.title || '알림'}</div>
        <div class="notification-body">${n.body || ''}</div>
      `;
      list.appendChild(link);
    });
  }
  if(badge){
    if(items.length){
      badge.style.display = 'inline-flex';
      badge.textContent = Math.min(items.length, 99);
    } else {
      badge.style.display = 'none';
    }
  }
}

function addNotificationsToCenter(items = []){
  if(!items || !items.length) return;
  let changed = false;
  items.forEach(n=>{
    const key = n.id ?? `${n.title}|${n.body}|${n.url}`;
    if(notificationCenter.seen.has(key)) return;
    notificationCenter.seen.add(key);
    notificationCenter.items.unshift({
      title: n.title || '새 알림',
      body: n.body || '',
      url: n.url || '/quiz/list'
    });
    changed = true;
  });
  if(notificationCenter.items.length > 20){
    notificationCenter.items = notificationCenter.items.slice(0, 20);
  }
  if(changed) renderNotificationCenter();
}

function setupNotificationBell(){
  notificationCenter.bell = document.getElementById('notificationBell');
  notificationCenter.panel = document.getElementById('notificationPanel');
  notificationCenter.list = document.getElementById('notificationList');
  notificationCenter.badge = document.getElementById('notificationBadge');
  notificationCenter.friendBadge = document.getElementById('friendBadge');
  if(!notificationCenter.bell || !notificationCenter.panel) return;

  notificationCenter.bell.addEventListener('click', (e)=>{
    e.preventDefault();
    const isOpen = notificationCenter.panel.classList.toggle('open');
    notificationCenter.panel.style.display = isOpen ? 'block' : 'none';
  });
  document.addEventListener('click', (e)=>{
    if(!notificationCenter.panel) return;
    if(e.target.closest('.notif-wrapper')) return;
    notificationCenter.panel.classList.remove('open');
    notificationCenter.panel.style.display = 'none';
  });
  renderNotificationCenter();
}

function setupFriendDropdown(){
  const btn = document.getElementById('friendBell');
  const panel = document.getElementById('friendPanel');
  const listEl = document.getElementById('friendRequestList');
  const csrf = document.querySelector('meta[name="_csrf"]')?.content || document.getElementById('friendCsrfParam')?.value || '';
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || document.getElementById('friendCsrfHeader')?.value || 'X-CSRF-TOKEN';
  const addForm = document.getElementById('friendAddForm');
  const shareInputs = document.querySelectorAll('[data-role="friend-share"]');
  if(!btn || !panel) return;

  const renderRequests = async () => {
    if(!listEl) return;
    listEl.innerHTML = '';
    try{
      const res = await fetch('/api/friends/inbox', { credentials:'same-origin' });
      if(!res.ok) return;
      const data = await res.json();
      const groupRes = await fetch('/api/groups/invite/inbox', { credentials:'same-origin' });
      let groupData = { received:[], sent:[] };
      if(groupRes.ok){
        groupData = await groupRes.json();
      }
      const recv = data.received || [];
      const sent = data.sent || [];
      const accepted = data.accepted || [];
      const shareReceived = data.shareReceived || [];
      const shareSent = data.shareSent || [];
      const shareAccepted = data.shareAccepted || [];
      const groupReceived = groupData.received || [];
      const groupSent = groupData.sent || [];
      if(!recv.length && !sent.length && !accepted.length && !groupReceived.length && !groupSent.length){
        listEl.innerHTML = '<div class="notification-empty">대기 중인 요청이 없습니다.</div>';
        const fb = notificationCenter.friendBadge;
        if(fb) fb.style.display = 'none';
        return;
      }
      const fb = notificationCenter.friendBadge;
      if(fb){
        const total = recv.length + accepted.length + shareReceived.length + shareAccepted.length + groupReceived.length;
        if(total > 0){
          fb.style.display = 'inline-flex';
          fb.textContent = Math.min(total,99);
        } else {
          fb.style.display = 'none';
        }
      }
      recv.forEach(item=>{
        const row = document.createElement('div');
        row.className = 'notification-item';
        row.innerHTML = `
          <div class="notification-title">${item.from} 님이 친구 요청을 보냈습니다.</div>
          <div class="notification-body">${new Date(item.createdAt).toLocaleString()}</div>
          <div style="margin-top:6px; display:flex; gap:6px;">
            <button class="btn btn-outline" data-role="accept" data-id="${item.id}" style="padding:4px 8px;">수락</button>
            <button class="btn btn-outline" data-role="reject" data-id="${item.id}" style="padding:4px 8px;">거절</button>
          </div>
        `;
        listEl.appendChild(row);
      });
      if(sent.length){
        const header = document.createElement('div');
        header.className = 'notification-header';
        header.textContent = '보낸 요청(대기 중)';
        listEl.appendChild(header);
        sent.forEach(item=>{
          const row = document.createElement('div');
          row.className = 'notification-item';
          row.innerHTML = `
            <div class="notification-title">${item.to} 님에게 요청 보냄</div>
            <div class="notification-body">${new Date(item.createdAt).toLocaleString()}</div>
          `;
          listEl.appendChild(row);
        });
      }
      if(accepted.length){
        const header = document.createElement('div');
        header.className = 'notification-header';
        header.textContent = '수락됨';
        listEl.appendChild(header);
        accepted.forEach(item=>{
          const row = document.createElement('div');
          row.className = 'notification-item';
          row.innerHTML = `
            <div class="notification-title">${item.to} 님이 친구 요청을 수락했습니다.</div>
            <div class="notification-body">${new Date(item.createdAt).toLocaleString()}</div>
          `;
          listEl.appendChild(row);
        });
      }
      if(shareReceived.length){
        const header = document.createElement('div');
        header.className = 'notification-header';
        header.textContent = '함께 풀기 요청';
        listEl.appendChild(header);
        shareReceived.forEach(item=>{
          const row = document.createElement('div');
          row.className = 'notification-item';
          row.innerHTML = `
            <div class="notification-title">${item.from} 님이 함께 풀기를 요청했습니다.</div>
            <div class="notification-body">${item.questionText}</div>
            <div style="margin-top:6px; display:flex; gap:6px;">
              <button class="btn btn-outline" data-role="share-accept" data-id="${item.id}" style="padding:4px 8px;">수락</button>
              <button class="btn btn-outline" data-role="share-reject" data-id="${item.id}" style="padding:4px 8px;">거절</button>
            </div>
          `;
          listEl.appendChild(row);
        });
      }
      if(shareSent.length){
        const header = document.createElement('div');
        header.className = 'notification-header';
        header.textContent = '보낸 함께 풀기(대기)';
        listEl.appendChild(header);
        shareSent.forEach(item=>{
          const row = document.createElement('div');
          row.className = 'notification-item';
          row.innerHTML = `
            <div class="notification-title">${item.to} 님에게 함께 풀기 요청</div>
            <div class="notification-body">${item.questionText}</div>
          `;
          listEl.appendChild(row);
        });
      }
      if(shareAccepted.length){
        const header = document.createElement('div');
        header.className = 'notification-header';
        header.textContent = '함께 풀기 수락됨';
        listEl.appendChild(header);
        shareAccepted.forEach(item=>{
          const row = document.createElement('div');
          row.className = 'notification-item';
          row.innerHTML = `
            <div class="notification-title">${item.to} 님이 함께 풀기 요청을 수락했습니다.</div>
            <div class="notification-body">${item.questionText}</div>
          `;
          listEl.appendChild(row);
        });
      }
      if(groupReceived.length){
        const header = document.createElement('div');
        header.className = 'notification-header';
        header.textContent = '그룹 초대';
        listEl.appendChild(header);
        groupReceived.forEach(item=>{
          const row = document.createElement('div');
          row.className = 'notification-item';
          row.innerHTML = `
            <div class="notification-title">${item.from} 님이 ${item.groupName} 그룹에 초대했습니다.</div>
            <div class="notification-body">코드: ${item.code} · ${new Date(item.createdAt).toLocaleString()}</div>
            <div style="margin-top:6px; display:flex; gap:6px;">
              <button class="btn btn-outline" data-role="group-accept" data-id="${item.id}" style="padding:4px 8px;">수락</button>
              <button class="btn btn-outline" data-role="group-reject" data-id="${item.id}" style="padding:4px 8px;">거절</button>
            </div>
          `;
          listEl.appendChild(row);
        });
      }
      if(groupSent.length){
        const header = document.createElement('div');
        header.className = 'notification-header';
        header.textContent = '보낸 그룹 초대(대기)';
        listEl.appendChild(header);
        groupSent.forEach(item=>{
          const row = document.createElement('div');
          row.className = 'notification-item';
          row.innerHTML = `
            <div class="notification-title">${item.to} 님에게 ${item.groupName} 초대</div>
            <div class="notification-body">${new Date(item.createdAt).toLocaleString()}</div>
          `;
          listEl.appendChild(row);
        });
      }
      listEl.querySelectorAll('[data-role="accept"]').forEach(btn=>{
        btn.addEventListener('click', async (e)=>{
          e.stopPropagation();
          const id = btn.dataset.id;
          await fetch(`/api/friends/request/${id}/accept`, {
            method:'POST',
            headers:{ [csrfHeader]: csrf }
          });
          renderRequests();
        });
      });
      listEl.querySelectorAll('[data-role="reject"]').forEach(btn=>{
        btn.addEventListener('click', async (e)=>{
          e.stopPropagation();
          const id = btn.dataset.id;
          await fetch(`/api/friends/request/${id}/reject`, {
            method:'POST',
            headers:{ [csrfHeader]: csrf }
          });
          renderRequests();
        });
      });
      listEl.querySelectorAll('[data-role="share-accept"]').forEach(btn=>{
        btn.addEventListener('click', async (e)=>{
          e.stopPropagation();
          const id = btn.dataset.id;
          await fetch(`/api/friends/share/${id}/accept`, {
            method:'POST',
            headers:{ [csrfHeader]: csrf }
          });
          renderRequests();
        });
      });
      listEl.querySelectorAll('[data-role="share-reject"]').forEach(btn=>{
        btn.addEventListener('click', async (e)=>{
          e.stopPropagation();
          const id = btn.dataset.id;
          await fetch(`/api/friends/share/${id}/reject`, {
            method:'POST',
            headers:{ [csrfHeader]: csrf }
          });
          renderRequests();
        });
      });
      listEl.querySelectorAll('[data-role="group-accept"]').forEach(btn=>{
        btn.addEventListener('click', async (e)=>{
          e.stopPropagation();
          const id = btn.dataset.id;
          await fetch(`/api/groups/invite/${id}/accept`, { method:'POST', headers:{ [csrfHeader]: csrf } });
          renderRequests();
        });
      });
      listEl.querySelectorAll('[data-role="group-reject"]').forEach(btn=>{
        btn.addEventListener('click', async (e)=>{
          e.stopPropagation();
          const id = btn.dataset.id;
          await fetch(`/api/groups/invite/${id}/reject`, { method:'POST', headers:{ [csrfHeader]: csrf } });
          renderRequests();
        });
      });
    }catch(e){ console.error(e); }
  };

  if(addForm){
    addForm.addEventListener('submit', async (e)=>{
      e.preventDefault();
      const username = addForm.querySelector('input[name="username"]')?.value || '';
      if(!username) return;
      await fetch('/api/friends/request', {
        method:'POST',
        headers:{
          'Content-Type':'application/x-www-form-urlencoded',
          [csrfHeader]: csrf
        },
        body:new URLSearchParams({ username })
      });
      addForm.querySelector('input[name="username"]').value='';
      renderRequests();
    });
  }

  shareInputs.forEach(input => {
    const btn = input.querySelector('button');
    const friendId = input.dataset.friendId;
    btn?.addEventListener('click', async (e)=>{
      e.preventDefault();
      const qid = input.querySelector('input')?.value;
      if(!qid) return;
      await fetch('/api/friends/share', {
        method:'POST',
        headers:{
          'Content-Type':'application/x-www-form-urlencoded',
          [csrfHeader]: csrf
        },
        body: new URLSearchParams({ friendId, questionId: qid })
      });
      input.querySelector('input').value='';
      alert('문제를 공유했습니다.');
    });
  });

  btn.addEventListener('click', (e)=>{
    e.preventDefault();
    const open = panel.classList.toggle('open');
    panel.style.display = open ? 'block' : 'none';
    if(open) renderRequests();
  });
  document.addEventListener('click', (e)=>{
    if(!panel) return;
    if(e.target.closest('.notif-wrapper')) return;
    panel.classList.remove('open');
    panel.style.display = 'none';
  });
}

function setupProfileDropdown(){
  const btn = document.getElementById('profileButton');
  const panel = document.getElementById('profilePanel');
  if(!btn || !panel) return;
  btn.addEventListener('click', (e)=>{
    e.preventDefault();
    const open = panel.classList.toggle('open');
    panel.style.display = open ? 'block' : 'none';
  });
  document.addEventListener('click', (e)=>{
    if(!panel) return;
    if(e.target.closest('.profile-wrapper')) return;
    panel.classList.remove('open');
    panel.style.display = 'none';
  });
}

// 폴링 기반 웹 알림 (서버 push 없이도 동작)
async function pollNotifications(){
  if(!('Notification' in window)) return;
  if(Notification.permission !== 'granted') return;
  try{
    const res = await fetch('/api/notifications/due', { credentials:'same-origin' });
    if(!res.ok) return;
    const items = await res.json();
    addNotificationsToCenter(items);
    items.forEach(n=>{
      const noti = new Notification(n.title || '새 문제 도착', { body:n.body || '', data:{url:n.url||'/quiz/list'} });
      noti.onclick = () => { window.open(n.data.url, '_blank'); };
    });
  }catch(e){
    console.error('Notification poll error', e);
  }
}

document.addEventListener('DOMContentLoaded', ()=>{
  if('Notification' in window){
    // 한 번만 요청
    if(Notification.permission === 'default'){
      Notification.requestPermission().then(perm=>{
        if(perm === 'granted') scheduleNotificationsNow();
      });
    } else if(Notification.permission === 'granted'){
      scheduleNotificationsNow();
    }
    setInterval(pollNotifications, 60_000);
    pollNotifications();
  }
  setupNotificationBell();
  setupFriendDropdown();
  setupProfileDropdown();
});
