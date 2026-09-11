const GATEWAY_API_BASE = 'http://localhost:8080';
const API_BASE = GATEWAY_API_BASE;
const BOOKING_API_BASE = GATEWAY_API_BASE;
const USER_API_BASE = GATEWAY_API_BASE;

const defaultEvents = [
  {
    id: 'seed-1',
    name: 'Midnight Echo Tour',
    artist: 'The Weeknd',
    location: 'Madison Square Garden, New York',
    eventDate: '2026-09-18T20:00:00',
    seats: [
      { row: 'A', seatNumber: 12, price: 66 },
      { row: 'A', seatNumber: 13, price: 82 },
      { row: 'B', seatNumber: 5, price: 92 },
      { row: 'B', seatNumber: 6, price: 108 }
    ]
  },
  {
    id: 'seed-2',
    name: 'City Lights Gala',
    artist: 'Imagine Dragons',
    location: 'Hollywood Bowl, Los Angeles',
    eventDate: '2026-09-20T19:30:00',
    seats: [
      { row: 'A', seatNumber: 2, price: 95 },
      { row: 'A', seatNumber: 3, price: 110 },
      { row: 'C', seatNumber: 7, price: 120 }
    ]
  }
];

let allEvents = [];
let selectedEvent = null;
let selectedSeat = null;
let selectedSeats = [];
let currentUser = null;
let authMode = 'login';
const USER_TICKETS_KEY = 'pulsepass-user-tickets';

const seatsBeingPaid = new Map();

function addDynamicStyles() {
  if (document.getElementById('dynamicAppStyles')) {
    return;
  }

  const style = document.createElement('style');
  style.id = 'dynamicAppStyles';

  style.textContent = `
    .seat-btn.available {
      background: #22c55e !important;
      border-color: #16a34a !important;
      color: white !important;
      cursor: pointer;
    }

    .seat-btn.available:hover {
      background: #16a34a !important;
      transform: translateY(-2px);
    }

    .seat-btn.booked,
    .seat-btn.paying {
      background: #ef4444 !important;
      border-color: #dc2626 !important;
      color: white !important;
      cursor: not-allowed;
      opacity: 1 !important;
    }

    .seat-btn.selected {
      background: #2563eb !important;
      border-color: #1d4ed8 !important;
      color: white !important;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.25);
      transform: translateY(-2px);
    }

    .seat-btn:disabled {
      opacity: 1 !important;
    }

    .seat-legend {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 20px;
      margin-top: 18px;
      flex-wrap: wrap;
      font-size: 13px;
      color: #6b7280;
    }

    .seat-legend-item {
      display: flex;
      align-items: center;
      gap: 7px;
    }

    .seat-legend-color {
      width: 16px;
      height: 16px;
      border-radius: 4px;
      display: inline-block;
    }

    .seat-legend-color.free {
      background: #22c55e;
    }

    .seat-legend-color.booked {
      background: #ef4444;
    }

    .seat-legend-color.selected {
      background: #2563eb;
    }

    .payment-notification {
      position: fixed;
      top: 28px;
      right: 28px;
      z-index: 99999;

      min-width: 340px;
      max-width: 460px;

      display: flex;
      align-items: center;
      gap: 16px;

      padding: 18px 22px;

      border-radius: 16px;

      background: white;

      box-shadow:
        0 20px 40px rgba(0, 0, 0, 0.15),
        0 5px 15px rgba(0, 0, 0, 0.08);

      transform: translateX(calc(100% + 50px));
      opacity: 0;

      transition:
        transform 0.35s ease,
        opacity 0.35s ease;
    }

    .payment-notification.show {
      transform: translateX(0);
      opacity: 1;
    }

    .payment-notification.success {
      border-left: 5px solid #22c55e;
    }

    .payment-notification.error {
      border-left: 5px solid #ef4444;
    }

    .notification-icon {
      width: 48px;
      height: 48px;
      min-width: 48px;

      border-radius: 50%;

      display: flex;
      align-items: center;
      justify-content: center;

      font-size: 27px;
      font-weight: 800;
    }

    .success .notification-icon {
      background: #dcfce7;
      color: #16a34a;
    }

    .error .notification-icon {
      background: #fee2e2;
      color: #dc2626;
    }

    .notification-content {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .notification-title {
      font-size: 16px;
      font-weight: 700;
      color: #111827;
    }

    .notification-text {
      font-size: 14px;
      color: #6b7280;
      line-height: 1.4;
    }

    .notification-close {
      margin-left: auto;

      border: none;
      background: transparent;

      font-size: 22px;
      color: #9ca3af;

      cursor: pointer;
      padding: 4px;
    }

    .notification-close:hover {
      color: #374151;
    }

    @media (max-width: 600px) {
      .payment-notification {
        left: 16px;
        right: 16px;
        top: 16px;

        min-width: 0;
        max-width: none;
      }
    }
  `;

  document.head.appendChild(style);
}

function showPaymentNotification(success, titleText = null, detailText = null) {
  const oldNotification = document.getElementById('paymentNotification');

  if (oldNotification) {
    oldNotification.remove();
  }

  const notification = document.createElement('div');

  notification.id = 'paymentNotification';
  notification.className = `payment-notification ${success ? 'success' : 'error'}`;

  if (success) {
    notification.innerHTML = `
      <div class="notification-icon">
        ✓
      </div>

      <div class="notification-content">
        <div class="notification-title">
          ${titleText || 'Платёж успешно завершён'}
        </div>
        ${detailText ? `<div class="notification-text">${detailText}</div>` : '<div class="notification-text">Билет подтверждён.</div>'}
      </div>

      <button
        type="button"
        class="notification-close"
        aria-label="Close"
      >
        ×
      </button>
    `;
  } else {
    notification.innerHTML = `
      <div class="notification-icon">
        ☹
      </div>

      <div class="notification-content">
        <div class="notification-title">
          ${titleText || 'Платёж не прошёл'}
        </div>
        <div class="notification-text">
          ${detailText || 'Проверьте данные карты и попробуйте ещё раз.'}
        </div>
      </div>

      <button
        type="button"
        class="notification-close"
        aria-label="Close"
      >
        ×
      </button>
    `;
  }

  document.body.appendChild(notification);

  const closeButton = notification.querySelector('.notification-close');

  closeButton.addEventListener('click', () => {
    hidePaymentNotification(notification);
  });

  requestAnimationFrame(() => {
    notification.classList.add('show');
  });

  setTimeout(() => {
    hidePaymentNotification(notification);
  }, 5000);
}

function hidePaymentNotification(notification) {
  if (!notification) {
    return;
  }

  notification.classList.remove('show');

  setTimeout(() => {
    if (notification.parentNode) {
      notification.remove();
    }
  }, 350);
}

function formatDate(dateString) {
  const date = new Date(dateString);

  return date
      .toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric'
      })
      .toUpperCase();
}

function formatLongDate(dateString) {
  return new Date(dateString).toLocaleString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  });
}

function getStartingPrice(seats = []) {
  if (!seats.length) {
    return 0;
  }

  return Math.min(
      ...seats.map((seat) => Number(seat.price || 0))
  );
}

function getCityLabel(location = '') {
  return location.split(',').slice(-1)[0].trim();
}

function getSeatKey(row, seatNumber) {
  const normalizedRow = String(row || '').trim().toUpperCase();
  const normalizedSeatNumber = Number(seatNumber);
  return `${normalizedRow}${normalizedSeatNumber}`;
}

function parseSeatKey(ticketKey) {
  const normalized = String(ticketKey || '').trim().toUpperCase();
  const match = normalized.match(/^([A-Z]+)(\d+)$/);

  if (!match) {
    return { row: '', seatNumber: null };
  }

  return {
    row: match[1],
    seatNumber: Number(match[2])
  };
}

function isSeatBeingPaid(eventId, row, seatNumber) {
  const eventSeats = seatsBeingPaid.get(String(eventId));

  if (!eventSeats) {
    return false;
  }

  return eventSeats.has(
      getSeatKey(row, seatNumber)
  );
}

function getBackendSeatStatus(seat) {
  if (!seat) {
    return 'AVAILABLE';
  }

  if (seat.booked === true) {
    return 'BOOKED';
  }

  if (seat.available === false) {
    return 'BOOKED';
  }

  const status = String(
      seat.status ||
      seat.bookingStatus ||
      seat.state ||
      ''
  ).toUpperCase();

  if (
      status === 'BOOKED' ||
      status === 'PAID' ||
      status === 'RESERVED' ||
      status === 'SOLD'
  ) {
    return 'BOOKED';
  }

  if (
      status === 'PENDING' ||
      status === 'PAYMENT' ||
      status === 'PAYING' ||
      status === 'PROCESSING' ||
      status === 'HOLD' ||
      status === 'HELD'
  ) {
    return 'PAYING';
  }

  return 'AVAILABLE';
}

async function hydrateSeatStatusesForEvents(events) {
  if (!Array.isArray(events)) {
    return events;
  }

  await Promise.all(events.map(async (event) => {
    if (!event || !event.id) {
      return event;
    }

    try {
      const response = await fetch(`${BOOKING_API_BASE}/api/bookings/events/${event.id}/seat-status`);

      if (!response.ok) {
        return event;
      }

      const seatStatuses = await response.json();
      const lookup = new Map((seatStatuses || []).map((seatStatus) => [
        `${seatStatus.row}-${seatStatus.seatNumber}`,
        seatStatus.status
      ]));

      (event.seats || []).forEach((seat) => {
        const key = `${seat.row}-${seat.seatNumber}`;
        const backendStatus = lookup.get(key);

        if (!backendStatus) {
          return;
        }

        seat.status = backendStatus;
        seat.available = backendStatus === 'AVAILABLE';
        seat.booked = ['RESERVED', 'SOLD'].includes(backendStatus);
      });
    } catch (error) {
      console.warn('Seat status sync failed for event', event.id, error);
    }

    return event;
  }));

  return events;
}

function getSeatMap(event) {
  const seats = event.seats || [];

  const seatLookup = new Map();

  seats.forEach((seat) => {
    const key = `${seat.row}-${seat.seatNumber}`;

    seatLookup.set(key, seat);
  });

  const rows = ['A', 'B', 'C', 'D', 'E', 'F'];
  const seatNumbers = Array.from(
      { length: 12 },
      (_, index) => index + 1
  );

  return rows.map((row) => ({
    row,

    seats: seatNumbers.map((seatNumber) => {
      const key = `${row}-${seatNumber}`;

      const seat = seatLookup.get(key);

      let status = seat
          ? getBackendSeatStatus(seat)
          : 'EMPTY';

      if (
          seat &&
          isSeatBeingPaid(
              event.id,
              row,
              seatNumber
          )
      ) {
        status = 'PAYING';
      }

      return {
        key,
        row,
        seatNumber,

        available: status === 'AVAILABLE',

        booked: status === 'BOOKED',

        paying: status === 'PAYING',

        price: seat
            ? Number(seat.price || 0)
            : 0,

        isSeat: Boolean(seat),

        status
      };
    })
  }));
}

function renderEvents(events) {
  const grid = document.querySelector('.event-grid');

  if (!grid) {
    return;
  }

  grid.innerHTML = events
      .map((event, index) => {
        const startingPrice = getStartingPrice(event.seats);

        const city = getCityLabel(event.location);

        const coverClass = `image-${(index % 4) + 1}`;

        return `
        <article
          class="event-card"
          data-event-id="${event.id}"
        >
          <div class="event-image ${coverClass}"></div>

          <div class="event-body">

            <div class="event-meta-row">
              <span class="date-badge">
                ${formatDate(event.eventDate)}
              </span>

              <span class="location">
                ${city}
              </span>
            </div>

            <h3>${event.name}</h3>

            <p>${event.location}</p>

            <div class="event-footer">
              <span class="tag">
                ${event.artist || 'Live'}
              </span>

              <strong>
                From $${startingPrice}
              </strong>
            </div>

            <button class="card-buy-btn" type="button" data-event-id="${event.id}">
              Купить билеты
            </button>

          </div>
        </article>
      `;
      })
      .join('');

  document.querySelectorAll('.event-card').forEach((card) => {
    card.addEventListener('click', (clickEvent) => {
      if (clickEvent.target.closest('.card-buy-btn')) {
        const eventId = clickEvent.target.closest('.card-buy-btn').dataset.eventId;
        const selectedEventData = allEvents.find((item) => String(item.id) === String(eventId));

        if (selectedEventData) {
          openEventModal(selectedEventData);
        }
        return;
      }

      const eventId = card.dataset.eventId;

      const selectedEventData = allEvents.find(
          (item) => String(item.id) === String(eventId)
      );

      if (selectedEventData) {
        openEventModal(selectedEventData);
      }
    });
  });
}

function openEventModal(event) {
  selectedEvent = event;
  selectedSeat = null;

  const modal = document.getElementById('eventModal');

  const hero = document.getElementById('modalHero');

  const title = document.getElementById('modalTitle');

  const meta = document.getElementById('modalMeta');

  const description = document.getElementById(
      'modalDescription'
  );

  const tag = document.getElementById('modalTag');

  const seatMap = document.getElementById('seatMap');

  if (seatMap) {
    seatMap.innerHTML = '';
  }

  const selectedSeatLabel =
      document.getElementById(
          'selectedSeatLabel'
      );

  const buyTicketBtn = document.getElementById('buyTicketBtn');

  const index = allEvents.findIndex(
      (item) => String(item.id) === String(event.id)
  );

  hero.style.backgroundImage =
      `linear-gradient(
      180deg,
      rgba(17,24,39,0.1),
      rgba(17,24,39,0.25)
    ),
    url(
      'https://images.unsplash.com/photo-${
          [
            '1501386761578-eac5c94b800a',
            '1492684223066-81342ee5ff30',
            '1501612780327-45045538702b',
            '1516280440614-37939bbacd81'
          ][index % 4]
      }?auto=format&fit=crop&w=1200&q=80'
    )`;

  title.textContent = event.name;

  meta.textContent =
      `${event.location} • ${formatLongDate(event.eventDate)}`;

  description.textContent =
      `${event.artist || 'Featured artist'} live at ` +
      `${event.location}. Secure your seat and enjoy ` +
      `the best experience in the venue.`;

  tag.textContent =
      event.artist || 'Live';

  const selectedSeatNames = selectedSeats
      .filter((seat) => seat && seat.eventId === event.id)
      .map((seat) => `${seat.row}${seat.seatNumber}`);

  selectedSeatLabel.textContent = selectedSeatNames.length
      ? selectedSeatNames.join(', ')
      : 'No seat selected';

  if (buyTicketBtn) {
    buyTicketBtn.disabled = selectedSeats.length === 0;
    buyTicketBtn.textContent = selectedSeats.length > 0 ? `Buy ticket(s) (${selectedSeats.length})` : 'Buy ticket';
  }

  const ownedTickets = getUserTickets()[String(event.id)] || [];
  const userOwnedSeatSet = new Set((ownedTickets).map((ticket) => String(ticket).trim().toUpperCase()));

  const seatLayout = getSeatMap(event);

  seatLayout.forEach((row) => {
    const rowEl = document.createElement('div');
    rowEl.className = 'seat-row';

    row.seats.forEach((seat) => {
      if (!seat.isSeat) {
        const empty = document.createElement('div');
        empty.style.width = '42px';
        empty.style.height = '42px';
        empty.style.visibility = 'hidden';
        rowEl.appendChild(empty);
        return;
      }

      const button = document.createElement('button');
      button.type = 'button';

      const seatKey = getSeatKey(seat.row, seat.seatNumber).toUpperCase();
      const isOwnedSeat = userOwnedSeatSet.has(seatKey);
      const isSeatBooked = Boolean(seat.booked) || isOwnedSeat || ['RESERVED', 'SOLD'].includes(String(seat.status || '').toUpperCase());

      if (isSeatBooked) {
        button.className = 'seat-btn booked';
        button.disabled = true;
        button.title = isOwnedSeat
            ? `${seat.row}${seat.seatNumber} • Ваш билет`
            : `${seat.row}${seat.seatNumber} • Забронировано`;
      } else if (seat.paying) {
        button.className = 'seat-btn paying';
        button.disabled = true;
        button.title = `${seat.row}${seat.seatNumber} • Оплата в процессе`;
      } else {
        button.className = 'seat-btn available';
        button.title = `${seat.row}${seat.seatNumber} • $${seat.price}`;
      }

      button.textContent = String(seat.seatNumber);
      button.dataset.row = String(seat.row);
      button.dataset.seatNumber = String(seat.seatNumber);
      button.dataset.price = String(seat.price);

      if (isSeatBooked || seat.paying) {
        rowEl.appendChild(button);
        return;
      }

      button.addEventListener('click', () => {
        const candidate = {
          eventId: event.id,
          row: seat.row,
          seatNumber: seat.seatNumber,
          price: Number(seat.price || 0),
          label: `${seat.row}${seat.seatNumber}`
        };

        const exists = selectedSeats.some((item) => item.eventId === event.id && item.row === seat.row && item.seatNumber === seat.seatNumber);

        if (exists) {
          selectedSeats = selectedSeats.filter((item) => !(item.eventId === event.id && item.row === seat.row && item.seatNumber === seat.seatNumber));
        } else {
          selectedSeats.push(candidate);
        }

        const selectedSeatNames = selectedSeats
            .filter((item) => item && item.eventId === event.id)
            .map((item) => `${item.row}${item.seatNumber}`);

        selectedSeatLabel.textContent = selectedSeatNames.length ? selectedSeatNames.join(', ') : 'No seat selected';

        document.querySelectorAll('.seat-btn').forEach((seatBtn) => {
          const rowValue = seatBtn.dataset.row;
          const seatNumberValue = Number(seatBtn.dataset.seatNumber);
          const selected = selectedSeats.some((item) => item.eventId === event.id && item.row === rowValue && item.seatNumber === seatNumberValue);
          seatBtn.classList.toggle('selected', selected);
        });

        if (buyTicketBtn) {
          buyTicketBtn.disabled = selectedSeats.length === 0;
          buyTicketBtn.textContent = selectedSeats.length > 0 ? `Buy ticket(s) (${selectedSeats.length})` : 'Buy ticket';
        }
      });

      rowEl.appendChild(button);
    });

    seatMap.appendChild(rowEl);
  });

  const oldLegend =
      document.querySelector('.seat-legend');

  if (oldLegend) {
    oldLegend.remove();
  }

  const legend =
      document.createElement('div');

  legend.className =
      'seat-legend';

  legend.innerHTML = `
    <div class="seat-legend-item">
      <span class="seat-legend-color free"></span>
      <span>Свободно</span>
    </div>

    <div class="seat-legend-item">
      <span class="seat-legend-color selected"></span>
      <span>Выбрано</span>
    </div>

    <div class="seat-legend-item">
      <span class="seat-legend-color booked"></span>
      <span>Забронировано / оплачивается</span>
    </div>
  `;

  seatMap.parentElement.appendChild(
      legend
  );

  modal.classList.remove('hidden');

  modal.setAttribute(
      'aria-hidden',
      'false'
  );
}

function closeModal(modalId) {
  const modal =
      document.getElementById(modalId);

  if (modal) {
    modal.classList.add('hidden');

    modal.setAttribute(
        'aria-hidden',
        'true'
    );
  }
}

function readCurrentUser() {
  try {
    const raw = localStorage.getItem('pulsepass-user');
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    console.warn('Failed to read user session:', error);
    return null;
  }
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1];
    if (!payload) {
      return null;
    }

    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    return JSON.parse(atob(padded));
  } catch (error) {
    return null;
  }
}

function isAccessTokenExpired(token) {
  const payload = decodeJwtPayload(token);
  if (!payload || !payload.exp) {
    return false;
  }

  return Date.now() >= payload.exp * 1000 - 30000;
}

async function ensureValidAccessToken() {
  if (!currentUser?.accessToken) {
    throw new Error('Authentication required');
  }

  if (!isAccessTokenExpired(currentUser.accessToken)) {
    return currentUser.accessToken;
  }

  if (!currentUser.refreshToken) {
    throw new Error('Session expired');
  }

  const response = await fetch(`${USER_API_BASE}/api/users/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: currentUser.refreshToken })
  });

  if (!response.ok) {
    logoutUser();
    throw new Error('Session expired. Please sign in again.');
  }

  const payload = await response.json();
  currentUser.accessToken = payload.accessToken;
  persistCurrentUser(currentUser);
  return payload.accessToken;
}

function persistCurrentUser(user) {
  currentUser = user;

  if (user) {
    localStorage.setItem('pulsepass-user', JSON.stringify({
      ...user,
      accessToken: user.accessToken,
      refreshToken: user.refreshToken
    }));
  } else {
    localStorage.removeItem('pulsepass-user');
  }

  const authToggleBtn = document.getElementById('authToggleBtn');

  if (authToggleBtn) {
    if (user) {
      authToggleBtn.textContent = `Profile: ${user.fullName || user.email || 'User'}`;
      authToggleBtn.classList.add('is-user');
    } else {
      authToggleBtn.textContent = 'Sign in';
      authToggleBtn.classList.remove('is-user');
    }
  }
}

function logoutUser() {
  currentUser = null;
  localStorage.removeItem('pulsepass-user');
  const authToggleBtn = document.getElementById('authToggleBtn');
  if (authToggleBtn) {
    authToggleBtn.textContent = 'Sign in';
    authToggleBtn.classList.remove('is-user');
  }

  const authStatus = document.getElementById('authStatus');
  if (authStatus) {
    authStatus.textContent = 'Вы вышли из системы.';
    authStatus.classList.add('visible');
  }

  const profileModal = document.getElementById('profileModal');
  if (profileModal) {
    profileModal.classList.add('hidden');
    profileModal.setAttribute('aria-hidden', 'true');
  }
}

async function loadUserProfile() {
  if (!currentUser?.id) {
    return;
  }

  try {
    const authToken = await ensureValidAccessToken();

    const [profileResponse, ordersResponse] = await Promise.all([
      fetch(`${USER_API_BASE}/api/users/profile/${currentUser.id}`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      }),
      fetch(`${USER_API_BASE}/api/users/${currentUser.id}/orders`, {
        headers: {
          'Authorization': `Bearer ${authToken}`
        }
      })
    ]);

    if (!profileResponse.ok) {
      throw new Error('Failed to load user profile');
    }

    const profile = await profileResponse.json();
    const orders = ordersResponse.ok ? await ordersResponse.json() : [];

    const profileName = document.getElementById('profileName');
    const profileEmail = document.getElementById('profileEmail');
    const profileSince = document.getElementById('profileSince');
    const profileOrders = document.getElementById('profileOrders');

    if (profileName) {
      profileName.textContent = profile.fullName || profile.email || 'User';
    }

    if (profileEmail) {
      profileEmail.textContent = profile.email || '—';
    }

    if (profileSince) {
      const createdAt = profile.createdAt ? new Date(profile.createdAt) : null;
      profileSince.textContent = createdAt ? createdAt.toLocaleDateString() : '—';
    }

    if (profileOrders) {
      if (!orders.length) {
        profileOrders.innerHTML = '<div class="empty-orders">У вас пока нет заказов. После покупки билеты появятся здесь.</div>';
        return;
      }

      profileOrders.innerHTML = orders.map((order) => {
        const seatLabel = String(order.seatLabel || 'Место');
        const row = seatLabel.replace(/\d+$/, '');
        const seatNumber = Number(seatLabel.replace(/\D/g, '')) || 0;
        const eventId = order.eventId || order.event?.id || selectedEvent?.id || '';

        return `
        <div class="order-item" data-order-event-id="${eventId}" data-order-seat-label="${seatLabel}">
          <div>
            <strong>${order.eventName || 'Мероприятие'}</strong>
            <small>${seatLabel} • ${new Date(order.purchasedAt).toLocaleDateString()}</small>
          </div>
          <div class="order-actions">
            <div class="ticket-pill">$${Number(order.totalPrice || 0).toFixed(2)}</div>
            <button class="return-order-btn" type="button" data-order-event-id="${eventId}" data-order-seat-row="${row}" data-order-seat-number="${seatNumber}">
              Вернуть
            </button>
          </div>
        </div>
      `;
      }).join('');

      profileOrders.querySelectorAll('.return-order-btn').forEach((button) => {
        button.addEventListener('click', async () => {
          const eventId = button.dataset.orderEventId;
          const row = button.dataset.orderSeatRow;
          const seatNumber = Number(button.dataset.orderSeatNumber || 0);

          if (!eventId || !row || !seatNumber) {
            return;
          }

          const event = allEvents.find((item) => String(item.id) === String(eventId));

          if (!event) {
            showPaymentNotification(false, 'Мероприятие не найдено', 'Не удалось найти событие для возврата.');
            return;
          }

          await ticketReturnClicked(event, row, seatNumber);
          await loadUserProfile();
        });
      });
    }
  } catch (error) {
    console.error('Failed to load profile:', error);
    const profileOrders = document.getElementById('profileOrders');
    if (profileOrders) {
      profileOrders.innerHTML = '<div class="empty-orders">Не удалось загрузить ваши заказы.</div>';
    }
  }
}

function openProfileModal() {
  const modal = document.getElementById('profileModal');
  if (!modal) {
    return;
  }

  if (!currentUser) {
    openAuthModal('login');
    return;
  }

  loadUserProfile();
  modal.classList.remove('hidden');
  modal.setAttribute('aria-hidden', 'false');
}

function closeProfileModal() {
  const modal = document.getElementById('profileModal');
  if (modal) {
    modal.classList.add('hidden');
    modal.setAttribute('aria-hidden', 'true');
  }
}

function openAuthModal(mode = 'login') {
  authMode = mode;

  const modal = document.getElementById('authModal');
  const registerNameField = document.getElementById('registerNameField');
  const submitBtn = document.getElementById('authSubmitBtn');
  const authTabs = document.querySelectorAll('.auth-tab');

  if (modal) {
    modal.classList.remove('hidden');
    modal.setAttribute('aria-hidden', 'false');
  }

  if (registerNameField) {
    registerNameField.classList.toggle('hidden', mode !== 'register');
  }

  if (submitBtn) {
    submitBtn.textContent = mode === 'register' ? 'Create account' : 'Sign in';
  }

  authTabs.forEach((tab) => {
    const isActive = tab.dataset.authView === mode;
    tab.classList.toggle('active', isActive);
  });
}

function closeAuthModal() {
  const modal = document.getElementById('authModal');

  if (modal) {
    modal.classList.add('hidden');
    modal.setAttribute('aria-hidden', 'true');
  }
}

function getUserTickets() {
  try {
    const storage = localStorage.getItem(USER_TICKETS_KEY);
    return storage ? JSON.parse(storage) : {};
  } catch (error) {
    return {};
  }
}

function saveUserTickets(data) {
  localStorage.setItem(USER_TICKETS_KEY, JSON.stringify(data));
}

function isUserOwnedSeat(eventId, row, seatNumber) {
  const eventTickets = getUserTickets()[String(eventId)] || [];
  const seatKey = getSeatKey(row, seatNumber);

  return eventTickets.some((ticket) => {
    const parsed = parseSeatKey(ticket);
    return parsed.row && parsed.seatNumber !== null && getSeatKey(parsed.row, parsed.seatNumber) === seatKey;
  });
}

function addUserOwnedSeat(eventId, row, seatNumber) {
  const tickets = getUserTickets();
  const key = String(eventId);
  const ticketKey = getSeatKey(row, seatNumber);
  const current = Array.isArray(tickets[key]) ? tickets[key] : [];
  const normalizedCurrent = current.map((item) => String(item).trim().toUpperCase());

  if (!normalizedCurrent.includes(ticketKey.toUpperCase())) {
    tickets[key] = [...current, ticketKey];
    saveUserTickets(tickets);
  }
}

function removeUserOwnedSeat(eventId, row, seatNumber) {
  const tickets = getUserTickets();
  const key = String(eventId);
  const ticketKey = getSeatKey(row, seatNumber);
  const current = Array.isArray(tickets[key]) ? tickets[key] : [];

  tickets[key] = current.filter((item) => String(item).trim().toUpperCase() !== ticketKey.toUpperCase());

  if (!tickets[key].length) {
    delete tickets[key];
  }

  saveUserTickets(tickets);
}

async function ticketReturnClicked(event, row, seatNumber) {
  const key = getSeatKey(row, seatNumber);
  const isOwned = isUserOwnedSeat(event.id, row, seatNumber);

  if (!isOwned || !currentUser?.id) {
    return;
  }

  try {
    const accessToken = await ensureValidAccessToken();
    const response = await fetch(`${BOOKING_API_BASE}/api/bookings/return`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${accessToken}`
      },
      body: JSON.stringify({
        eventId: event.id,
        userId: currentUser.id,
        seatRow: row,
        seatNumber: Number(seatNumber)
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Ticket return failed');
    }

    const seat = (event.seats || []).find((item) => item.row === row && Number(item.seatNumber) === Number(seatNumber));
    if (seat) {
      seat.status = 'AVAILABLE';
      seat.available = true;
      seat.booked = false;
      delete seat.paying;
    }

    removeUserOwnedSeat(event.id, row, seatNumber);
    showPaymentNotification(true, 'Билет возвращён', `${key} снова доступен.`);
    await loadEvents();
    openEventModal(event);
  } catch (error) {
    console.error('Ticket return failed:', error);
    showPaymentNotification(false, 'Возврат не выполнен', 'Билет не удалось вернуть сейчас.');
  }
}

function openPaymentModal() {
  if (!selectedEvent || selectedSeats.length === 0) {
    showPaymentNotification(false);

    return;
  }

  if (!currentUser) {
    openAuthModal('login');
    const authStatus = document.getElementById('authStatus');
    if (authStatus) {
      authStatus.textContent = 'Please sign in before purchasing tickets.';
      authStatus.classList.add('visible');
    }
    return;
  }

  const summary =
      document.getElementById(
          'paymentSummary'
      );

  const total = selectedSeats.reduce((sum, seat) => sum + Number(seat.price || 0), 0);
  const labels = selectedSeats.map((seat) => seat.label).join(', ');

  summary.textContent =
      `${labels} • $${total}`;

  document
      .getElementById('paymentModal')
      .classList.remove('hidden');
}

async function searchEvents(event) {
  event.preventDefault();

  const artist =
      document
          .getElementById('artistFilter')
          .value
          .trim();

  const location =
      document
          .getElementById('locationFilter')
          .value
          .trim();

  const date =
      document
          .getElementById('dateFilter')
          .value;

  const params =
      new URLSearchParams();

  if (artist) {
    params.set(
        'artist',
        artist
    );
  }

  if (location) {
    params.set(
        'location',
        location
    );
  }

  if (date) {
    params.set(
        'date',
        date
    );
  }

  try {
    const response =
        await fetch(
            `${API_BASE}/api/events?${params.toString()}`
        );

    if (!response.ok) {
      throw new Error(
          `HTTP ${response.status}`
      );
    }

    const events =
        await response.json();

    allEvents =
        events.length
            ? events
            : [];

    await hydrateSeatStatusesForEvents(allEvents);

    renderEvents(allEvents);

  } catch (error) {
    console.error(
        'Search failed:',
        error
    );

    allEvents =
        defaultEvents.filter((item) => {

          const matchesArtist =
              !artist ||
              (item.artist || item.name)
                  .toLowerCase()
                  .includes(
                      artist.toLowerCase()
                  );

          const matchesLocation =
              !location ||
              item.location
                  .toLowerCase()
                  .includes(
                      location.toLowerCase()
                  );

          const matchesDate =
              !date ||
              item.eventDate.startsWith(
                  date
              );

          return (
              matchesArtist &&
              matchesLocation &&
              matchesDate
          );
        });

    renderEvents(allEvents);
  }
}

async function loadEvents() {
  try {
    const response =
        await fetch(
            `${API_BASE}/api/events`
        );

    if (!response.ok) {
      throw new Error(
          `HTTP ${response.status}`
      );
    }

    allEvents =
        await response.json();

    if (!allEvents.length) {
      allEvents =
          defaultEvents;
    }

    await hydrateSeatStatusesForEvents(allEvents);

    renderEvents(
        allEvents
    );

  } catch (error) {
    console.error(
        'Failed to load events:',
        error
    );

    allEvents =
        defaultEvents;

    renderEvents(
        defaultEvents
    );
  }
}

function markSeatAsPaying(
    eventId,
    row,
    seatNumber
) {
  const id = String(eventId);

  if (!seatsBeingPaid.has(id)) {
    seatsBeingPaid.set(
        id,
        new Set()
    );
  }

  seatsBeingPaid
      .get(id)
      .add(
          getSeatKey(
              row,
              seatNumber
          )
      );
}

function unmarkSeatAsPaying(
    eventId,
    row,
    seatNumber
) {
  const id = String(eventId);

  const eventSeats =
      seatsBeingPaid.get(id);

  if (!eventSeats) {
    return;
  }

  eventSeats.delete(
      getSeatKey(
          row,
          seatNumber
      )
  );

  if (eventSeats.size === 0) {
    seatsBeingPaid.delete(id);
  }
}

function markSeatAsBooked(
    event,
    selectedSeat
) {
  if (!event || !selectedSeat) {
    return;
  }

  const seat =
      (event.seats || []).find(
          (item) =>
              String(item.row) ===
              String(selectedSeat.row) &&
              Number(item.seatNumber) ===
              Number(selectedSeat.seatNumber)
      );

  if (!seat) {
    return;
  }

  seat.status = 'BOOKED';
  seat.booked = true;
  seat.available = false;
}

function markSeatAsAvailable(
    event,
    selectedSeat
) {
  if (!event || !selectedSeat) {
    return;
  }

  const seat =
      (event.seats || []).find(
          (item) =>
              String(item.row) ===
              String(selectedSeat.row) &&
              Number(item.seatNumber) ===
              Number(selectedSeat.seatNumber)
      );

  if (!seat) {
    return;
  }

  delete seat.booked;

  seat.available = true;
  seat.status = 'AVAILABLE';
}

function rerenderCurrentEventSeats() {
  if (!selectedEvent) {
    return;
  }

  openEventModal(
      selectedEvent
  );
}

document.addEventListener(
    'DOMContentLoaded',
    () => {

      addDynamicStyles();

      const chips =
          document.querySelectorAll(
              '.chip'
          );

      const searchForm =
          document.getElementById(
              'searchForm'
          );

      const buyButton =
          document.getElementById(
              'buyTicketBtn'
          );

      const paymentForm =
          document.getElementById(
              'paymentForm'
          );

      const authToggleBtn =
          document.getElementById(
              'authToggleBtn'
          );

      const authForm =
          document.getElementById(
              'authForm'
          );

      const authTabs =
          document.querySelectorAll(
              '.auth-tab'
          );

      const authStatus =
          document.getElementById(
              'authStatus'
          );

      const authCloseButtons =
          document.querySelectorAll(
              '[data-close="auth"]'
          );
      const profileCloseButtons =
          document.querySelectorAll(
              '[data-close="profile"]'
          );
      const profileLogoutBtn =
          document.getElementById('profileLogoutBtn');

      currentUser = readCurrentUser();
      persistCurrentUser(currentUser);

      authCloseButtons.forEach((button) => {
        button.addEventListener('click', () => closeAuthModal());
      });

      profileCloseButtons.forEach((button) => {
        button.addEventListener('click', () => closeProfileModal());
      });

      if (profileLogoutBtn) {
        profileLogoutBtn.addEventListener('click', () => {
          logoutUser();
          closeProfileModal();
          if (authStatus) {
            authStatus.textContent = 'Вы вышли из системы.';
            authStatus.classList.add('visible');
          }
        });
      }

      if (authToggleBtn) {
        authToggleBtn.addEventListener('click', () => {
          if (currentUser) {
            openProfileModal();
            return;
          }

          openAuthModal('login');
        });
      }

      authTabs.forEach((tab) => {
        tab.addEventListener('click', () => {
          openAuthModal(tab.dataset.authView || 'login');
        });
      });

      if (authForm) {
        authForm.addEventListener('submit', async (event) => {
          event.preventDefault();

          const email = document.getElementById('authEmail')?.value?.trim();
          const password = document.getElementById('authPassword')?.value || '';
          const fullName = document.getElementById('registerName')?.value?.trim() || '';
          const endpoint = authMode === 'register' ? 'register' : 'login';

          try {
            const response = await fetch(`${USER_API_BASE}/api/users/${endpoint}`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                email,
                fullName,
                password
              })
            });

            const payload = await response.json().catch(() => ({}));

            if (!response.ok) {
              throw new Error(payload.message || payload.error || 'Authentication failed');
            }

            persistCurrentUser(payload);

            if (authStatus) {
              authStatus.textContent = authMode === 'register'
                  ? 'Регистрация успешна. Теперь можно покупать билеты.'
                  : 'С возвращением!';
              authStatus.classList.add('visible');
            }

            closeAuthModal();

            if (selectedEvent && selectedSeats.length > 0) {
              openPaymentModal();
            }
          } catch (error) {
            console.error('Auth failed:', error);
            if (authStatus) {
              authStatus.textContent = error.message || 'Не удалось выполнить вход';
              authStatus.classList.add('visible');
            }
          }
        });
      }

      chips.forEach((chip) => {
        chip.addEventListener(
            'click',
            () => {

              chips.forEach(
                  (item) =>
                      item.classList.remove(
                          'active'
                      )
              );

              chip.classList.add(
                  'active'
              );
            }
        );
      });

      if (searchForm) {
        searchForm.addEventListener(
            'submit',
            searchEvents
        );
      }

      if (buyButton) {
        buyButton.disabled = true;
        buyButton.addEventListener(
            'click',
            openPaymentModal
        );
      }

      if (paymentForm) {
        paymentForm.addEventListener(
            'submit',
            async (event) => {

              event.preventDefault();

              if (
                  !selectedEvent ||
                  selectedSeats.length === 0
              ) {
                showPaymentNotification(
                    false
                );

                return;
              }

              const paymentEvent =
                  selectedEvent;

              const paymentSeats = selectedSeats
                  .filter((seat) => seat && seat.eventId === paymentEvent.id)
                  .map((seat) => ({ ...seat }));

              const submitButton =
                  paymentForm.querySelector(
                      'button[type="submit"]'
                  );

              const originalText =
                  submitButton.textContent;

              submitButton.disabled =
                  true;

              submitButton.textContent =
                  'Processing...';

              paymentSeats.forEach((paymentSeat) => {
                markSeatAsPaying(
                    paymentEvent.id,
                    paymentSeat.row,
                    paymentSeat.seatNumber
                );
              });

              if (selectedEvent) {
                openEventModal(
                    selectedEvent
                );
              }

              try {
                const results = [];

                for (const paymentSeat of paymentSeats) {
                  const userId = currentUser?.id || 'frontend-user';
              const accessToken = currentUser?.accessToken;

              const bookingResponse = await fetch(`${BOOKING_API_BASE}/api/bookings`, {
                    method: 'POST',
                    headers: {
                      'Content-Type': 'application/json',
                      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {})
                    },
                    body: JSON.stringify({
                      eventId: paymentEvent.id,
                      userId,
                      seatRow: paymentSeat.row,
                      seatNumber: paymentSeat.seatNumber
                    })
                  });

                  if (!bookingResponse.ok) {
                    const errorData = await bookingResponse.text();
                    throw new Error(errorData || 'Booking failed');
                  }

                  const bookingData = await bookingResponse.json();
                  const finalStatus = await pollBookingStatus(bookingData.bookingId);

                  results.push({
                    seat: paymentSeat,
                    status: finalStatus
                  });
                }

                const hasFailure = results.some((result) => result.status === 'CANCELLED');
                const hasSuccess = results.some((result) => result.status === 'PAID');

                if (hasSuccess) {
                  if (currentUser?.id) {
                    for (const { seat } of results) {
                      addUserOwnedSeat(paymentEvent.id, seat.row, seat.seatNumber);
                      try {
                        const authToken = await ensureValidAccessToken();
                        await fetch(`${USER_API_BASE}/api/users/orders`, {
                          method: 'POST',
                          headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${authToken}`
                          },
                          body: JSON.stringify({
                            userId: currentUser.id,
                            eventId: paymentEvent.id,
                            eventName: paymentEvent.name,
                            seatLabel: `${seat.row}${seat.seatNumber}`,
                            totalPrice: Number(seat.price || 0)
                          })
                        });
                      } catch (orderError) {
                        console.warn('Could not store order history:', orderError);
                      }
                    }
                  }

                  results.forEach(({ seat }) => {
                    unmarkSeatAsPaying(paymentEvent.id, seat.row, seat.seatNumber);
                    markSeatAsBooked(paymentEvent, seat);
                  });
                  showPaymentNotification(true, 'Платёж успешно завершён', 'Билет подтверждён.');
                  closeModal('paymentModal');
                  selectedSeat = null;
                  selectedSeats = [];
                  if (buyButton) {
                    buyButton.disabled = true;
                    buyButton.textContent = 'Buy ticket';
                  }
                  selectedEvent = null;
                  loadEvents();
                  return;
                }

                if (hasFailure) {
                  showPaymentNotification(false);
                  return;
                }

                showPaymentNotification(false);

              } catch (error) {
                console.error('Booking/payment error:', error);
                showPaymentNotification(false);

              } finally {
                submitButton.disabled = false;
                submitButton.textContent = originalText;
              }
            }
        );
      }

      document
          .querySelectorAll(
              '[data-close="event"]'
          )
          .forEach((button) => {

            button.addEventListener(
                'click',
                () =>
                    closeModal(
                        'eventModal'
                    )
            );
          });

      document
          .querySelectorAll(
              '[data-close="payment"]'
          )
          .forEach((button) => {

            button.addEventListener(
                'click',
                () =>
                    closeModal(
                        'paymentModal'
                    )
            );
          });

      const cardNumber =
          document.getElementById(
              'cardNumber'
          );

      if (cardNumber) {
        cardNumber.addEventListener(
            'input',
            (event) => {

              const value =
                  event.target.value
                      .replace(/\D/g, '')
                      .slice(0, 16);

              event.target.value =
                  value
                      .replace(
                          /(.{4})/g,
                          '$1 '
                      )
                      .trim();
            }
        );
      }

      const expiry =
          document.getElementById(
              'expiry'
          );

      if (expiry) {
        expiry.addEventListener(
            'input',
            (event) => {

              let value =
                  event.target.value
                      .replace(/\D/g, '')
                      .slice(0, 4);

              if (value.length > 2) {
                value =
                    `${value.slice(0, 2)}/` +
                    `${value.slice(2)}`;
              }

              event.target.value =
                  value;
            }
        );
      }

      loadEvents();
    }
);

async function pollBookingStatus(
    bookingId
) {
  const deadline =
      Date.now() + 20000;

  while (
      Date.now() < deadline
      ) {

    try {
      const response =
          await fetch(
              `${BOOKING_API_BASE}/api/bookings/${bookingId}/status`
          );

      if (!response.ok) {
        return 'PENDING';
      }

      const payload =
          await response.json();

      if (
          payload.status === 'PAID' ||
          payload.status === 'CANCELLED'
      ) {
        return payload.status;
      }

    } catch (error) {

      console.error(
          'Status poll error:',
          error
      );
    }

    await new Promise(
        (resolve) =>
            setTimeout(
                resolve,
                1000
            )
    );
  }

  return 'PENDING';
}