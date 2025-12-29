document.addEventListener('DOMContentLoaded', function () {
    const notificationBadge = document.getElementById('notificationBadge');
    const notificationsListContainer = document.getElementById('notificationItemsContainer');
    const markAllAsReadBtn = document.getElementById('markAllAsReadBtn');
    const notificationsDropdownContainer = document.getElementById('notificationsDropdownContainer');

    let stompClient = null;
    let areNotificationsCurrentlyLoading = false;

    // Define basePath using the global variable
    const basePath = (typeof window.appContextPath !== 'undefined' && window.appContextPath !== '/' ? window.appContextPath : '') + '/';


    console.log('notifications.js: Script loaded. Logged in username:', window.loggedInUsername, 'BasePath:', basePath);


    if (!notificationsDropdownContainer || !notificationBadge || !notificationsListContainer || !markAllAsReadBtn) {
        console.warn('notifications.js: One or more essential notification UI elements are missing.');
        return;
    }

    function updateUnreadCount(count) {
        if (count > 0) {
            notificationBadge.textContent = count;
            notificationBadge.style.display = 'inline-block';
        } else {
            notificationBadge.textContent = '0';
            notificationBadge.style.display = 'none';
        }
    }

    async function fetchInitialUnreadCount() {
        console.log('notifications.js: Fetching initial unread count via REST...');
        try {
            const response = await fetch(basePath + 'api/notifications/unread-count');
            if (!response.ok) {
                console.error('notifications.js: Failed to fetch initial unread count. Status:', response.status);
                if (response.status === 401 && stompClient) { stompClient.disconnect(); }
                return;
            }
            const data = await response.json();
            updateUnreadCount(data.unreadCount);
        } catch (error) {
            console.error('notifications.js: Error fetching initial unread count:', error);
        }
    }

    async function fetchAndRenderNotifications() {
        if (areNotificationsCurrentlyLoading) return;
        areNotificationsCurrentlyLoading = true;
        notificationsListContainer.innerHTML = '<li><a class="dropdown-item text-muted fst-italic">Loading...</a></li>';
        console.log('notifications.js: Fetching notifications list via REST...');
        try {
            const response = await fetch(basePath + 'api/notifications?size=7');
            if (!response.ok) {
                notificationsListContainer.innerHTML = '<li><a class="dropdown-item text-danger">Failed to load.</a></li>';
                console.error('notifications.js: Failed to fetch notifications. Status:', response.status, await response.text());
                return;
            }
            const notifications = await response.json();
            renderNotifications(notifications, true);
        } catch (error) {
            notificationsListContainer.innerHTML = '<li><a class="dropdown-item text-danger">Error loading.</a></li>';
            console.error('notifications.js: Error fetching and rendering notifications:', error);
        } finally {
            areNotificationsCurrentlyLoading = false;
        }
    }

    function renderNotifications(notifications, clearExisting = true) {
        if (clearExisting) {
            notificationsListContainer.innerHTML = '';
        }

        if (!notifications || notifications.length === 0) {
            if (clearExisting) {
                 notificationsListContainer.innerHTML = '<li><a class="dropdown-item text-muted fst-italic">No notifications.</a></li>';
            }
            return;
        }

        notifications.forEach(notification => prependNotificationToList(notification));
    }

    function prependNotificationToList(notification) {
        const listItem = document.createElement('li');
        const link = document.createElement('a');
        link.classList.add('dropdown-item', 'notification-item', 'py-2');
        if (notification.status === 'UNREAD') {
            link.classList.add('fw-bold');
        }
        // Ensure link is context path aware if it's an internal link
        link.href = notification.link ? (notification.link.startsWith('/') ? basePath + notification.link.substring(1) : notification.link) : '#!';
        link.dataset.notificationId = notification.id;

        const messageDiv = document.createElement('div');
        messageDiv.style.whiteSpace = 'normal';
        messageDiv.textContent = notification.message;

        const timeDiv = document.createElement('div');
        timeDiv.classList.add('text-muted', 'small', 'mt-1');
        try {
            timeDiv.textContent = new Date(notification.createdAt).toLocaleString(navigator.language || 'en-US', { 
                month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit', hour12: true 
            });
        } catch (e) {
            timeDiv.textContent = notification.createdAt;
        }

        link.appendChild(messageDiv);
        link.appendChild(timeDiv);
        listItem.appendChild(link);

        if (notificationsListContainer.firstChild && (notificationsListContainer.firstChild.textContent === "Loading..." || notificationsListContainer.firstChild.textContent === "No notifications." )) {
            notificationsListContainer.innerHTML = '';
        }
        notificationsListContainer.prepend(listItem);


        link.addEventListener('click', async function(event) {
            if (link.getAttribute('href') === '#!') event.preventDefault();
            if (notification.status === 'UNREAD') { 
                await markAsRead(notification.id, link);
            }
            // If it's an internal link, and we prepended basePath, it should now navigate correctly.
        });
    }

    async function markAsRead(notificationId, linkElement) {
        console.log('notifications.js: Marking notification as read:', notificationId);
        try {
            const response = await fetch(basePath + `api/notifications/mark-as-read/${notificationId}`, {
                method: 'POST',
            });
            if (response.ok) {
                if (linkElement) linkElement.classList.remove('fw-bold');
            } else {
                console.error('notifications.js: Failed to mark notification as read. Status:', response.status);
            }
        } catch (error) {
            console.error('notifications.js: Error marking notification as read:', error);
        }
    }

    markAllAsReadBtn.addEventListener('click', async function(event) {
        event.preventDefault();
        console.log('notifications.js: Marking all as read...');
        try {
            const response = await fetch(basePath + 'api/notifications/mark-all-as-read', {
                method: 'POST',
            });
            if (response.ok) {
                fetchAndRenderNotifications();
            } else {
                console.error('notifications.js: Failed to mark all as read. Status:', response.status);
            }
        } catch (error) {
            console.error('notifications.js: Error marking all as read:', error);
        }
    });

    function connectWebSocket() {
        if (!window.loggedInUsername) {
            console.warn("notifications.js: No loggedInUsername found. WebSocket connection not attempted.");
            fetchInitialUnreadCount();
            return;
        }

        console.log('notifications.js: Attempting to connect to WebSocket on path: ' + basePath + 'ws-notifications');
        const socket = new SockJS(basePath + 'ws-notifications'); 
        stompClient = Stomp.over(socket);

        stompClient.connect({}, function (frame) {
            console.log('notifications.js: Connected to WebSocket: ' + frame);

            stompClient.subscribe('/user/queue/notifications', function (message) {
                console.log('notifications.js: Received WebSocket message (new notification):', message.body);
                try {
                    const notificationPayload = JSON.parse(message.body);
                    prependNotificationToList(notificationPayload);
                    updateUnreadCount(notificationPayload.unreadCount);
                } catch (e) {
                    console.error("Error parsing WebSocket notification message:", e, message.body);
                }
            });

            stompClient.subscribe('/user/queue/notification-count-update', function (message) {
                console.log('notifications.js: Received WebSocket message (count update):', message.body);
                try {
                    const payload = JSON.parse(message.body);
                    updateUnreadCount(payload.unreadCount);
                } catch(e) {
                    console.error("Error parsing WebSocket count update message:", e, message.body);
                }
            });

            fetchInitialUnreadCount();

        }, function(error) {
            console.error('notifications.js: STOMP error: ', error);
            console.warn('notifications.js: WebSocket connection failed. Falling back to REST polling for unread count.');
            fetchInitialUnreadCount();
        });
    }

    if (notificationsDropdownContainer) {
        notificationsDropdownContainer.addEventListener('show.bs.dropdown', function () {
            console.log('notifications.js: Notification dropdown shown, fetching list via REST.');
            fetchAndRenderNotifications();
        });
    }
    connectWebSocket();
});