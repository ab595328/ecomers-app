import React, { useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle,
  Clock,
  CreditCard,
  FileText,
  KeyRound,
  LayoutDashboard,
  LogOut,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  Star,
  Trash2,
  Truck,
  Users as UsersIcon,
  Settings,
  Ticket
} from 'lucide-react';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  onSnapshot,
  setDoc,
  updateDoc
} from 'firebase/firestore';
import { signInWithEmailAndPassword, signOut as firebaseSignOut } from 'firebase/auth';
import { db, auth } from './firebase';

const ORDER_STATUSES = ['Pending', 'Processing', 'Ready to Deliver', 'Shipped', 'Delivered', 'Cancelled', 'Seller Reject Requested'];
const ROLE_FILTERS = ['All', 'User', 'Seller', 'DeliveryPartner', 'Admin'];
const RETURN_WINDOW_DAYS = 7;
const DAY_MS = 24 * 60 * 60 * 1000;
const WITHDRAWAL_STATUS_ACTIONS = [
  { label: 'Pending', status: 'Pending', className: 'pending' },
  { label: 'Success', status: 'Paid', className: 'verified' },
  { label: 'Rejected', status: 'Failed', className: 'cancelled' }
];

const defaultProducts = [
  { id: 1, name: 'ZYL Sound Pro Wireless ANC', price: 129.99, originalPrice: 189.99, rating: 4.8, category: 'Electronics', imageUrlName: 'img_hero_banner', description: 'Premium high-fidelity wireless spatial audio headphones with leading hybrid Active Noise Cancelation. Styled in matte black with dynamic metallic accents.', isFeatured: true, sellerEmail: 'seller@store.com', extraImages: '' },
  { id: 2, name: 'ZYL Active Sport Sync Watch', price: 199.99, originalPrice: 249.99, rating: 4.6, category: 'Electronics', imageUrlName: '', description: 'Always-on AMOLED wellness assistant monitor with precise multi-sport tracking, sleep telemetry, and rapid 5-day continuous charging capability.', isFeatured: true, sellerEmail: 'seller@store.com', extraImages: '' },
  { id: 3, name: 'Premium Farms Organic Apples (1kg)', price: 4.99, originalPrice: 6.99, rating: 4.9, category: 'Fresh Products', imageUrlName: '', description: 'Crispy, hand-picked seasonal honeycomb organic apples. Sourced sustainably from local highland green farms directly to your table of freshness.', isFeatured: true, sellerEmail: 'seller@store.com', extraImages: '' },
  { id: 4, name: 'Farm-Fresh Avocados (Pack of 3)', price: 5.49, originalPrice: 7.99, rating: 4.7, category: 'Fresh Products', imageUrlName: '', description: 'Buttery local green Hass avocados ripe and ready to serve. Wealthy with nutrients, vitamins, and pure monounsaturated wellness lipids.', isFeatured: false, sellerEmail: 'seller@store.com', extraImages: '' },
  { id: 5, name: 'Organic Handpicked Strawberries (500g)', price: 6.99, originalPrice: 8.99, rating: 4.8, category: 'Fresh Products', imageUrlName: '', description: 'Sweet, delicious organic berries cultivated with care. Selected for supreme taste, vibrant crimson look, and ideal ripeness levels.', isFeatured: true, sellerEmail: 'seller@store.com', extraImages: '' },
  { id: 6, name: 'Emerald Velocity Retro Sneakers', price: 79.99, originalPrice: 119.99, rating: 4.7, category: 'Fashion', imageUrlName: '', description: 'Vibrant emerald accents paired with breathable slate mesh and durable vulcanized running heels. Engineered for day-to-day dynamic strides.', isFeatured: true, sellerEmail: 'seller@store.com', extraImages: '' },
  { id: 7, name: 'Classic Forest Leather Wallet', price: 34.99, originalPrice: 49.99, rating: 4.5, category: 'Fashion', imageUrlName: '', description: 'Genuine handcrafted full-grain green premium leather wallet. Offers precise built-in card slips and security shielding lines.', isFeatured: false, sellerEmail: 'seller@store.com', extraImages: '' },
  { id: 8, name: 'Aroma Brew Smart Drip Coffee Maker', price: 59.99, originalPrice: 89.99, rating: 4.4, category: 'Home & Kitchen', imageUrlName: '', description: 'Sleek compact programmable thermal drip brewing machine. Prepares rich bold roast flavor directly into double-walled glass mugs.', isFeatured: false, sellerEmail: 'seller@store.com', extraImages: '' },
  { id: 9, name: 'TurboCrisp Digital Air Fryer', price: 99.99, originalPrice: 149.99, rating: 4.8, category: 'Home & Kitchen', imageUrlName: '', description: 'Superheated high-density air vortex crisping technology. Cooks crunchy golden wings, chips, and snacks with up to 90% reduced fat oil.', isFeatured: true, sellerEmail: 'seller@store.com', extraImages: '' }
];

const defaultUsers = [
  { email: 'admin@bazaar.com', name: 'System Administrator', password: 'admin123', role: 'Admin', isPlusMember: true, savedAddress: '00123 Green Bazaar Lane, Eco City, 54002', notificationsEnabled: true, selectedLanguage: 'English' },
  { email: 'buyer@bazaar.com', name: 'Green Buyer', role: 'User', isPlusMember: true, savedAddress: '22 Market Street, Eco City', phone: '+91 8888811111' },
  { email: 'seller@store.com', name: 'Organic Farms Co.', role: 'Seller', isSellerVerified: true, isSellerVerificationPending: false, shopName: 'Organic Farms', shopAddress: 'Green Valley Farm Road', sellerMobile: '+91 7777711111', sellerGstNumber: 'GST-ZVB-1001' },
  { email: 'pending.seller@store.com', name: 'Fresh Cart Seller', role: 'Seller', isSellerVerified: false, isSellerVerificationPending: true, shopName: 'Fresh Cart', shopAddress: 'Unit 8, Local Bazaar', sellerMobile: '+91 7777722222' },
  { email: 'rider@bazaar.com', name: 'Fast Courier Rider', role: 'DeliveryPartner', isDeliveryPartnerVerified: true, deliveryMobile: '+91 9999911111', deliveryVehicleType: 'Electric Scooter', deliveryVehicleNumber: 'DL-3C-EC-9999', deliveryEmergencyContact: '+91 9999922222' },
  { email: 'pending.rider@bazaar.com', name: 'New Delivery Partner', role: 'DeliveryPartner', isDeliveryPartnerVerified: false, deliveryMobile: '+91 9999933333', deliveryVehicleType: 'Bike', deliveryVehicleNumber: 'DL-4S-NP-2211' }
];


const defaultOrders = [
  {
    orderId: 'ORD-9872',
    email: 'buyer@bazaar.com',
    orderDate: Date.now() - 3600000 * 2,
    totalAmount: 134.98,
    status: 'Processing',
    itemsSummary: '1x ZYL Sound Pro Wireless ANC, 1x Premium Farms Organic Apples (1kg)',
    productQuantities: '1:1,3:1',
    paymentMode: 'Card ending in 4242',
    deliveryAddress: '22 Market Street, Eco City',
    couponApplied: 'GREEN10',
    deliveryPartnerEmail: '',
    deliveryStatus: '',
    sellerConfirmed: false
  },
  {
    orderId: 'ORD-5431',
    email: 'buyer@bazaar.com',
    orderDate: Date.now() - 3600000 * (24 * 8),
    totalAmount: 4.99,
    status: 'Delivered',
    itemsSummary: '1x Premium Farms Organic Apples (1kg)',
    productQuantities: '3:1',
    paymentMode: 'Wallet',
    deliveryAddress: '22 Market Street, Eco City',
    deliveryPartnerEmail: 'rider@bazaar.com',
    deliveryStatus: 'Delivered',
    sellerConfirmed: true,
    deliveredAt: Date.now() - 3600000 * (24 * 8)
  },
  {
    orderId: 'ORD-1122',
    email: 'buyer@bazaar.com',
    orderDate: Date.now() - 3600000 * 12,
    totalAmount: 139.98,
    status: 'Delivered',
    itemsSummary: '1x ZYL Sound Pro Wireless ANC',
    productQuantities: '1:1',
    paymentMode: 'Card ending in 1111',
    deliveryAddress: '22 Market Street, Eco City',
    deliveryPartnerEmail: 'rider@bazaar.com',
    deliveryStatus: 'Delivered',
    sellerConfirmed: true,
    deliveredAt: Date.now() - 3600000 * 12,
    returnRequestsJson: JSON.stringify([
      {
        id: 'RET-9901',
        productId: '1',
        productName: 'ZYL Sound Pro Wireless ANC',
        customerEmail: 'buyer@bazaar.com',
        sellerEmail: 'seller@store.com',
        returnAmount: 129.99,
        deliveryFee: 10.00,
        reason: 'Faulty ANC feature',
        status: 'Pending',
        requestDate: Date.now() - 3600000 * 2
      }
    ])
  }
];

const defaultCategories = [
  { name: 'Electronics', isReturnable: true },
  { name: 'Fashion', isReturnable: true },
  { name: 'Fresh Products', isReturnable: true },
  { name: 'Home & Kitchen', isReturnable: true },
  { name: 'Food', isReturnable: false },
  { name: 'Vegetables', isReturnable: false }
];

const defaultWithdrawalRequests = [
  {
    id: 'WITH-101',
    accountEmail: 'seller@store.com',
    accountRole: 'Seller',
    amount: 1500.00,
    bankDetails: 'State Bank of India | A/C: 30981247921 | IFSC: SBIN0001234',
    status: 'Pending',
    requestDate: Date.now() - 3600000 * 6,
    payoutId: ''
  },
  {
    id: 'WITH-102',
    accountEmail: 'rider@bazaar.com',
    accountRole: 'DeliveryPartner',
    amount: 450.00,
    bankDetails: 'HDFC Bank | A/C: 501009871234 | IFSC: HDFC0000123',
    status: 'Paid',
    requestDate: Date.now() - 3600000 * 48,
    paidAt: Date.now() - 3600000 * 24,
    payoutId: 'PAY-880129'
  }
];

function roleLabel(role) {
  if (role === 'DeliveryPartner') return 'Delivery Partner';
  return role || 'User';
}

function statusClass(status) {
  return (status || 'pending').toLowerCase().replaceAll(' ', '-');
}

function formatCurrency(value) {
  return `₹${Number(value || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;
}

function parseProductQuantities(value) {
  if (!value || typeof value !== 'string') return new Map();
  return new Map(
    value
      .split(',')
      .map(entry => {
        const [rawId, rawQty] = entry.split(':');
        const productId = Number(rawId);
        const quantity = Number(rawQty);
        return Number.isFinite(productId) && Number.isFinite(quantity) && quantity > 0
          ? [productId, quantity]
          : null;
      })
      .filter(Boolean)
  );
}

function parseSummaryLine(line) {
  const text = String(line || '').trim();
  const leadingQty = text.match(/^(\d+)\s*x\s*(.+)$/i);
  if (leadingQty) return { name: leadingQty[2].trim(), quantity: Number(leadingQty[1]) || 1 };

  const trailingQty = text.match(/^(.+?)\s*x\s*(\d+)$/i);
  if (trailingQty) return { name: trailingQty[1].trim(), quantity: Number(trailingQty[2]) || 1 };

  return { name: text, quantity: 1 };
}

function parseReturnRequests(value) {
  if (!value || typeof value !== 'string') return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function getDisplayNameByEmail(users, email, fallback = 'N/A') {
  const user = users.find(u => (u.email || '').toLowerCase() === String(email || '').toLowerCase());
  return user?.shopName || user?.name || email || fallback;
}

function getUserByEmail(users, email) {
  return users.find(u => (u.email || '').toLowerCase() === String(email || '').toLowerCase());
}

function getBankDetails(user) {
  if (!user) return 'Bank details not available';
  return user.sellerBankAccount || user.deliveryBankAccount || 'Bank details not available';
}

function isDeliveredOrder(order) {
  return String(order.status || '').toLowerCase() === 'delivered' ||
    String(order.deliveryStatus || '').toLowerCase() === 'delivered';
}

function getReturnWindowEnd(order) {
  const deliveredAt = Number(order.deliveredAt || order.deliveryDate || order.orderDeliveredAt || order.orderDate || 0);
  return deliveredAt > 0 ? deliveredAt + RETURN_WINDOW_DAYS * DAY_MS : 0;
}

function isReturnWindowClosed(order) {
  const windowEnd = getReturnWindowEnd(order);
  return isDeliveredOrder(order) && windowEnd > 0 && Date.now() >= windowEnd;
}

function withdrawalStatusLabel(status) {
  if (status === 'Paid') return 'Success';
  if (status === 'Failed') return 'Rejected';
  if (status === 'Processing') return 'Processing';
  return 'Pending';
}

function withdrawalStatusClass(status) {
  if (status === 'Paid') return 'verified';
  if (status === 'Failed') return 'cancelled';
  return 'pending';
}

function buildSettlementEntries(orders, products, users) {
  const productsById = new Map(products.map(product => [Number(product.id), product]));
  const productsByName = new Map(products.map(product => [String(product.name || '').toLowerCase(), product]));

  return orders.map(order => {
    const quantityMap = parseProductQuantities(order.productQuantities);
    let items = Array.from(quantityMap.entries()).map(([productId, quantity]) => {
      const product = productsById.get(Number(productId));
      const unitPrice = Number(product?.price || 0);
      return {
        productId,
        productName: product?.name || `Product #${productId}`,
        sellerEmail: product?.sellerEmail || '',
        sellerName: getDisplayNameByEmail(users, product?.sellerEmail, product?.sellerEmail || 'System Store'),
        quantity,
        unitPrice,
        grossAmount: unitPrice * quantity
      };
    });

    if (items.length === 0) {
      items = String(order.itemsSummary || '')
        .split(',')
        .map(parseSummaryLine)
        .filter(item => item.name)
        .map(item => {
          const product = productsByName.get(item.name.toLowerCase());
          const unitPrice = Number(product?.price || 0);
          return {
            productId: product?.id || '',
            productName: product?.name || item.name,
            sellerEmail: product?.sellerEmail || '',
            sellerName: getDisplayNameByEmail(users, product?.sellerEmail, product?.sellerEmail || 'System Store'),
            quantity: item.quantity,
            unitPrice,
            grossAmount: unitPrice * item.quantity
          };
        });
    }

    const grossProductsAmount = items.reduce((sum, item) => sum + item.grossAmount, 0);
    const orderItemsAmount = Number(order.itemsAmount || 0);
    const totalAmount = Number(order.totalAmount || 0);
    const deliveryEarning = Number(order.deliveryCharge || 0) ||
      (orderItemsAmount > 0 ? Math.max(totalAmount - orderItemsAmount, 0) : 0);
    const sellerPool = orderItemsAmount > 0
      ? orderItemsAmount
      : (grossProductsAmount > 0 ? grossProductsAmount : Math.max(totalAmount - deliveryEarning, 0));

    const settledItems = items.map(item => ({
      ...item,
      sellerReceivable: grossProductsAmount > 0
        ? (item.grossAmount / grossProductsAmount) * sellerPool
        : item.grossAmount
    }));

    const returns = parseReturnRequests(order.returnRequestsJson)
      .filter(request => request && request.status !== 'Rejected')
      .map(request => {
        const sellerEmail = request.sellerEmail || productsById.get(Number(request.productId))?.sellerEmail || '';
        const sellerName = getDisplayNameByEmail(users, sellerEmail, sellerEmail || 'System Store');
        const returnAmount = Number(request.returnAmount || 0);
        const deliveryFee = Number(request.deliveryFee || 0);
        return {
          ...request,
          sellerEmail,
          sellerName,
          returnAmount,
          deliveryFee,
          debitAmount: returnAmount + deliveryFee,
          isCompleted: request.status === 'Completed'
        };
      });

    return {
      order,
      items: settledItems,
      returns,
      buyerEmail: order.email || '',
      buyerName: getDisplayNameByEmail(users, order.email, order.email || 'Customer'),
      totalPaid: totalAmount,
      sellerReceivable: settledItems.reduce((sum, item) => sum + item.sellerReceivable, 0),
      deliveryPartnerEmail: order.deliveryPartnerEmail || '',
      deliveryPartnerName: getDisplayNameByEmail(users, order.deliveryPartnerEmail, order.deliveryPartnerEmail || 'Not accepted'),
      deliveryEarning,
      returnWindowEnd: getReturnWindowEnd(order),
      payoutReady: isReturnWindowClosed(order)
    };
  });
}

function buildPayoutAccounts(entries, users) {
  const accounts = new Map();

  const ensureAccount = (role, email, name) => {
    const key = `${role}:${email || name}`;
    if (!accounts.has(key)) {
      const user = getUserByEmail(users, email);
      accounts.set(key, {
        key,
        role,
        email,
        name: name || getDisplayNameByEmail(users, email, email || 'Account'),
        bankDetails: getBankDetails(user),
        readyAmount: 0,
        pendingAmount: 0,
        returnDebits: 0,
        expectedReturnDebits: 0,
        orders: new Set()
      });
    }
    return accounts.get(key);
  };

  entries.forEach(entry => {
    if (!isDeliveredOrder(entry.order)) return;

    entry.items.forEach(item => {
      const account = ensureAccount('Seller', item.sellerEmail, item.sellerName);
      const completedDebits = entry.returns
        .filter(request => request.isCompleted && (
          Number(request.productId || 0) === Number(item.productId || 0) ||
          (!request.productId && request.sellerEmail === item.sellerEmail)
        ))
        .reduce((sum, request) => sum + request.debitAmount, 0);
      const expectedDebits = entry.returns
        .filter(request => !request.isCompleted && (
          Number(request.productId || 0) === Number(item.productId || 0) ||
          (!request.productId && request.sellerEmail === item.sellerEmail)
        ))
        .reduce((sum, request) => sum + request.debitAmount, 0);
      const netAmount = Math.max(item.sellerReceivable - completedDebits, 0);

      if (entry.payoutReady) account.readyAmount += netAmount;
      else account.pendingAmount += netAmount;
      account.returnDebits += completedDebits;
      account.expectedReturnDebits += expectedDebits;
      account.orders.add(entry.order.orderId);
    });

    if (entry.deliveryPartnerEmail) {
      const account = ensureAccount('DeliveryPartner', entry.deliveryPartnerEmail, entry.deliveryPartnerName);
      const returnPickupEarnings = entry.returns
        .filter(request => request.isCompleted && request.deliveryPartnerEmail === entry.deliveryPartnerEmail)
        .reduce((sum, request) => sum + request.deliveryFee, 0);
      const totalDelivery = entry.deliveryEarning + returnPickupEarnings;

      if (entry.payoutReady) account.readyAmount += totalDelivery;
      else account.pendingAmount += totalDelivery;
      account.orders.add(entry.order.orderId);
    }

    entry.returns
      .filter(request => request.isCompleted && request.deliveryPartnerEmail && request.deliveryPartnerEmail !== entry.deliveryPartnerEmail)
      .forEach(request => {
        const account = ensureAccount(
          'DeliveryPartner',
          request.deliveryPartnerEmail,
          getDisplayNameByEmail(users, request.deliveryPartnerEmail, request.deliveryPartnerEmail)
        );
        if (entry.payoutReady) account.readyAmount += request.deliveryFee;
        else account.pendingAmount += request.deliveryFee;
        account.orders.add(`${entry.order.orderId} return`);
      });
  });

  return Array.from(accounts.values())
    .map(account => ({ ...account, orders: Array.from(account.orders) }))
    .filter(account => account.readyAmount > 0 || account.pendingAmount > 0 || account.returnDebits > 0 || account.expectedReturnDebits > 0)
    .sort((a, b) => b.readyAmount - a.readyAmount || b.pendingAmount - a.pendingAmount);
}

function buildReturnRequestRows(orders, products, users) {
  const productsById = new Map(products.map(product => [Number(product.id), product]));

  return orders.flatMap(order => (
    parseReturnRequests(order.returnRequestsJson).map(request => {
      const product = productsById.get(Number(request.productId));
      const sellerEmail = request.sellerEmail || product?.sellerEmail || '';
      const deliveryPartnerEmail = request.deliveryPartnerEmail || '';
      const returnAmount = Number(request.returnAmount || product?.price || 0);
      const deliveryFee = Number(request.deliveryFee || 0);

      return {
        ...request,
        orderId: order.orderId,
        order,
        productName: request.productName || product?.name || `Product #${request.productId || 'N/A'}`,
        sellerEmail,
        sellerName: getDisplayNameByEmail(users, sellerEmail, sellerEmail || 'System Store'),
        customerEmail: request.customerEmail || order.email || '',
        customerName: getDisplayNameByEmail(users, request.customerEmail || order.email, request.customerEmail || order.email || 'Customer'),
        deliveryPartnerEmail,
        deliveryPartnerName: deliveryPartnerEmail ? getDisplayNameByEmail(users, deliveryPartnerEmail, deliveryPartnerEmail) : 'Not accepted',
        returnAmount,
        deliveryFee,
        debitAmount: returnAmount + deliveryFee
      };
    })
  )).sort((a, b) => Number(b.requestDate || 0) - Number(a.requestDate || 0));
}

function DetailLine({ label, value }) {
  if (value === undefined || value === null || value === '') return null;
  return (
    <p>
      <strong>{label}:</strong> {String(value)}
    </p>
  );
}

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [authUser, setAuthUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [loginEmail, setLoginEmail] = useState('admin@bazaar.com');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginError, setLoginError] = useState('');
  const [loginSubmitting, setLoginSubmitting] = useState(false);

  const [users, setUsers] = useState([]);
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [categories, setCategories] = useState([]);
  const [withdrawalRequests, setWithdrawalRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [dbError, setDbError] = useState(null);
  const [useMockData, setUseMockData] = useState(false);
  const [dbStatusMsg, setDbStatusMsg] = useState('');

  const [userSearch, setUserSearch] = useState('');
  const [userRoleFilter, setUserRoleFilter] = useState('All');
  const [orderSearch, setOrderSearch] = useState('');
  const [orderStatusFilter, setOrderStatusFilter] = useState('All');
  const [returnSearch, setReturnSearch] = useState('');
  const [returnStatusFilter, setReturnStatusFilter] = useState('All');
  const [expandedUserEmail, setExpandedUserEmail] = useState('');
  const [expandedOrderId, setExpandedOrderId] = useState('');
  const [appConfig, setAppConfig] = useState({});
  const [serviceCitiesText, setServiceCitiesText] = useState('');
  const [servicePincodesText, setServicePincodesText] = useState('');
  const [payoutDelayHours, setPayoutDelayHours] = useState('24');
  const [newCategoryName, setNewCategoryName] = useState('');
  const [newCategoryReturnable, setNewCategoryReturnable] = useState(true);

  const [showProductModal, setShowProductModal] = useState(false);
  const [newProductName, setNewProductName] = useState('');
  const [newProductPrice, setNewProductPrice] = useState('');
  const [newProductOrigPrice, setNewProductOrigPrice] = useState('');
  const [newProductCat, setNewProductCat] = useState('');
  const [newProductDesc, setNewProductDesc] = useState('');
  const [newProductSeller, setNewProductSeller] = useState('admin@bazaar.com');
  const [newProductFeatured, setNewProductFeatured] = useState(false);

  // Coupon management states
  const [coupons, setCoupons] = useState([]);
  const [showCouponModal, setShowCouponModal] = useState(false);
  const [newCouponCode, setNewCouponCode] = useState('');
  const [newCouponDiscount, setNewCouponDiscount] = useState('');
  const [newCouponMinOrder, setNewCouponMinOrder] = useState('');
  const [newCouponMaxDiscount, setNewCouponMaxDiscount] = useState('');
  const [newCouponDesc, setNewCouponDesc] = useState('');
  const [newCouponActive, setNewCouponActive] = useState(true);

  useEffect(() => {
    const savedSession = window.localStorage.getItem('bazaarAdminSession');
    const isDemo = window.localStorage.getItem('bazaarAdminDemoMode') === 'true';
    if (isDemo) {
      setUseMockData(true);
    }
    if (savedSession) {
      try {
        const sessionUser = JSON.parse(savedSession);
        if (sessionUser?.email && sessionUser?.role === 'Admin') {
          setAuthUser(sessionUser);
        }
      } catch (error) {
        console.warn('Invalid admin session:', error);
        window.localStorage.removeItem('bazaarAdminSession');
      }
    }
    setAuthLoading(false);
  }, []);

  useEffect(() => {
    if (!authUser && !useMockData) {
      setUsers([]);
      setProducts([]);
      setOrders([]);
      setCoupons([]);
      setLoading(false);
      return undefined;
    }

    if (useMockData) {
      setUsers(defaultUsers);
      setProducts(defaultProducts);
      setOrders(defaultOrders);
      setCategories(defaultCategories);
      setWithdrawalRequests(defaultWithdrawalRequests);
      setCoupons([]);
      setLoading(false);
      return undefined;
    }

    let unsubscribeUsers = () => { };
    let unsubscribeProducts = () => { };
    let unsubscribeOrders = () => { };
    let unsubscribeConfig = () => { };
    let unsubscribeCoupons = () => { };
    let unsubscribeCategories = () => { };
    let unsubscribeWithdrawals = () => { };

    try {
      setLoading(true);
      unsubscribeUsers = onSnapshot(
        collection(db, 'users'),
        snapshot => {
          const list = snapshot.docs.map(userDoc => ({ ...userDoc.data(), email: userDoc.id || userDoc.data().email }));
          setUsers(list.sort((a, b) => (a.role || '').localeCompare(b.role || '') || (a.email || '').localeCompare(b.email || '')));
          setDbError(null);
        },
        err => {
          console.warn('Firestore users sync failed:', err);
          setDbError('Unable to connect to Firebase. Please check Firestore configuration and rules.');
          setLoading(false);
        }
      );

      unsubscribeProducts = onSnapshot(
        collection(db, 'products'),
        snapshot => {
          const list = snapshot.docs.map(productDoc => ({ ...productDoc.data() }));
          setProducts(list.sort((a, b) => Number(a.id || 0) - Number(b.id || 0)));
        },
        err => {
          console.warn('Firestore products sync failed:', err);
          setDbError('Unable to read products from Firebase.');
        }
      );

      unsubscribeOrders = onSnapshot(
        collection(db, 'orders'),
        snapshot => {
          const list = snapshot.docs.map(orderDoc => ({ ...orderDoc.data(), orderId: orderDoc.data().orderId || orderDoc.id }));
          setOrders(list.sort((a, b) => Number(b.orderDate || 0) - Number(a.orderDate || 0)));
          setLoading(false);
        },
        err => {
          console.warn('Firestore orders sync failed:', err);
          setDbError('Unable to read orders from Firebase.');
          setLoading(false);
        }
      );

      unsubscribeCoupons = onSnapshot(
        collection(db, 'coupons'),
        snapshot => {
          const list = snapshot.docs.map(couponDoc => ({ ...couponDoc.data(), code: couponDoc.data().code || couponDoc.id }));
          setCoupons(list);
        },
        err => {
          console.warn('Firestore coupons sync failed:', err);
        }
      );

      unsubscribeCategories = onSnapshot(
        collection(db, 'categories'),
        snapshot => {
          const list = snapshot.docs.map(categoryDoc => ({
            name: categoryDoc.data().name || categoryDoc.id,
            isReturnable: categoryDoc.data().isReturnable !== false
          }));
          setCategories(list.sort((a, b) => a.name.localeCompare(b.name)));
        },
        err => console.warn('Firestore categories sync failed:', err)
      );

      unsubscribeWithdrawals = onSnapshot(
        collection(db, 'withdrawal_requests'),
        snapshot => {
          const list = snapshot.docs.map(withdrawDoc => ({
            ...withdrawDoc.data(),
            id: withdrawDoc.id,
            accountEmail: withdrawDoc.data().accountEmail || withdrawDoc.data().deliveryPartnerEmail || ''
          }));
          setWithdrawalRequests(list.sort((a, b) => Number(b.requestDate || 0) - Number(a.requestDate || 0)));
        },
        err => console.warn('Firestore withdrawals sync failed:', err)
      );

      unsubscribeConfig = onSnapshot(doc(db, 'app_config', 'main'), configDoc => {
        const config = configDoc.exists() ? configDoc.data() : {};
        setAppConfig(config);
        setServiceCitiesText((config.serviceCities || []).join(', '));
        setServicePincodesText((config.servicePincodes || []).join(', '));
        setPayoutDelayHours(String(config.payoutDelayHours ?? 24));
      });
    } catch (error) {
      console.warn('Firebase snapshot initialization error:', error);
      setDbError('Invalid Firebase configuration.');
      setLoading(false);
    }

    return () => {
      unsubscribeUsers();
      unsubscribeProducts();
      unsubscribeOrders();
      unsubscribeConfig();
      unsubscribeCoupons();
      unsubscribeCategories();
      unsubscribeWithdrawals();
    };
  }, [authUser, useMockData]);

  const activeSellersCount = users.filter(u => u.role === 'Seller' && u.isSellerVerified).length;
  const pendingSellersCount = users.filter(u => u.role === 'Seller' && (u.isSellerVerificationPending || !u.isSellerVerified)).length;
  const pendingPartnersCount = users.filter(u => u.role === 'DeliveryPartner' && !u.isDeliveryPartnerVerified).length;
  const buyerCount = users.filter(u => !u.role || u.role === 'User').length;
  const sellerCount = users.filter(u => u.role === 'Seller').length;
  const partnerCount = users.filter(u => u.role === 'DeliveryPartner').length;

  const totalRevenue = orders
    .filter(o => ['Delivered', 'Processing', 'Ready to Deliver', 'Shipped'].includes(o.status))
    .reduce((sum, o) => sum + Number(o.totalAmount || 0), 0);

  const settlementEntries = useMemo(
    () => buildSettlementEntries(orders, products, users),
    [orders, products, users]
  );

  const payoutAccounts = useMemo(
    () => buildPayoutAccounts(settlementEntries, users),
    [settlementEntries, users]
  );

  const settlementTotals = useMemo(() => {
    const returnDebit = settlementEntries.reduce(
      (sum, entry) => sum + entry.returns.filter(request => request.isCompleted).reduce((total, request) => total + request.debitAmount, 0),
      0
    );
    const expectedReturnDebit = settlementEntries.reduce(
      (sum, entry) => sum + entry.returns.filter(request => !request.isCompleted).reduce((total, request) => total + request.debitAmount, 0),
      0
    );
    const payoutReady = payoutAccounts.reduce((sum, account) => sum + account.readyAmount, 0);
    const payoutPending = payoutAccounts.reduce((sum, account) => sum + account.pendingAmount, 0);

    return settlementEntries.reduce(
      (totals, entry) => ({
        paid: totals.paid + entry.totalPaid,
        sellers: totals.sellers + entry.sellerReceivable,
        delivery: totals.delivery + entry.deliveryEarning,
        returnDebit,
        expectedReturnDebit,
        payoutReady,
        payoutPending
      }),
      { paid: 0, sellers: 0, delivery: 0, returnDebit, expectedReturnDebit, payoutReady, payoutPending }
    );
  }, [payoutAccounts, settlementEntries]);

  const returnRows = useMemo(
    () => buildReturnRequestRows(orders, products, users),
    [orders, products, users]
  );

  const pendingReturnCount = returnRows.filter(request => (request.status || 'Pending') === 'Pending').length;

  const filteredUsers = useMemo(() => {
    const query = userSearch.trim().toLowerCase();
    return users.filter(user => {
      const matchesRole = userRoleFilter === 'All' || (user.role || 'User') === userRoleFilter;
      const searchBlob = [user.name, user.email, user.phone, user.shopName, user.shopAddress, user.deliveryMobile, user.deliveryVehicleNumber]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return matchesRole && (!query || searchBlob.includes(query));
    });
  }, [userRoleFilter, userSearch, users]);

  const filteredOrders = useMemo(() => {
    const query = orderSearch.trim().toLowerCase();
    return orders.filter(order => {
      const matchesStatus = orderStatusFilter === 'All' || order.status === orderStatusFilter;
      const searchBlob = [order.orderId, order.email, order.itemsSummary, order.deliveryAddress, order.paymentMode, order.deliveryPartnerEmail]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return matchesStatus && (!query || searchBlob.includes(query));
    });
  }, [orderSearch, orderStatusFilter, orders]);

  const filteredReturnRows = useMemo(() => {
    const query = returnSearch.trim().toLowerCase();
    return returnRows.filter(request => {
      const status = request.status || 'Pending';
      const matchesStatus = returnStatusFilter === 'All' || status === returnStatusFilter;
      const searchBlob = [
        request.id,
        request.orderId,
        request.productName,
        request.customerEmail,
        request.sellerEmail,
        request.deliveryPartnerEmail,
        request.reason,
        status
      ].filter(Boolean).join(' ').toLowerCase();
      return matchesStatus && (!query || searchBlob.includes(query));
    });
  }, [returnRows, returnSearch, returnStatusFilter]);

  const handleLogin = async event => {
    event.preventDefault();
    setLoginError('');
    setLoginSubmitting(true);
    try {
      const email = loginEmail.trim().toLowerCase();
      // First sign in with Firebase Auth so the client gets authenticated
      await signInWithEmailAndPassword(auth, email, loginPassword);

      // Fetch the admin details (this check is now authorized because the client is logged in)
      const adminSnap = await getDoc(doc(db, 'users', email));

      if (!adminSnap.exists()) {
        await firebaseSignOut(auth);
        setLoginError('No admin user found with this email.');
        return;
      }

      const adminUser = { ...adminSnap.data(), email: adminSnap.id || email };
      if (adminUser.role !== 'Admin') {
        await firebaseSignOut(auth);
        setLoginError('This account is not marked as Admin.');
        return;
      }

      if ((adminUser.password || '') !== loginPassword) {
        await firebaseSignOut(auth);
        setLoginError('Wrong password.');
        return;
      }

      const sessionUser = {
        email: adminUser.email,
        name: adminUser.name || 'Admin',
        role: adminUser.role
      };
      window.localStorage.setItem('bazaarAdminSession', JSON.stringify(sessionUser));
      setAuthUser(sessionUser);
    } catch (error) {
      setLoginError(error.message || 'Unable to login. Check Firebase config and Firestore access.');
    } finally {
      setLoginSubmitting(false);
    }
  };

  const handleDemoLogin = () => {
    const sessionUser = {
      email: 'admin@bazaar.com',
      name: 'Demo Administrator',
      role: 'Admin'
    };
    window.localStorage.setItem('bazaarAdminSession', JSON.stringify(sessionUser));
    window.localStorage.setItem('bazaarAdminDemoMode', 'true');
    setUseMockData(true);
    setAuthUser(sessionUser);
  };

  const handleLogout = async () => {
    try {
      await firebaseSignOut(auth);
    } catch (e) {
      console.warn('Firebase auth signout failed:', e);
    }
    window.localStorage.removeItem('bazaarAdminSession');
    window.localStorage.removeItem('bazaarAdminDemoMode');
    setAuthUser(null);
    setUseMockData(false);
    setDbError(null);
  };

  const _seedFirestoreDatabase = async () => {
    if (useMockData) {
      alert('Please connect to your live Firebase project to seed data.');
      return;
    }
    if (!window.confirm('This will seed default users, products, orders, and categories to your Firestore database. Continue?')) return;

    try {
      setLoading(true);
      setDbStatusMsg('Seeding database. Please wait...');
      for (const u of defaultUsers) await setDoc(doc(db, 'users', u.email), u);
      for (const p of defaultProducts) await setDoc(doc(db, 'products', p.id.toString()), p);
      for (const o of defaultOrders) await setDoc(doc(db, 'orders', o.orderId), o);
      for (const c of defaultCategories) await setDoc(doc(db, 'categories', c.name), c);
      setDbStatusMsg('Database successfully seeded with default catalog data.');
      setTimeout(() => setDbStatusMsg(''), 5000);
    } catch (e) {
      alert(`Error seeding Firestore: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  const _exportDatabaseToJson = () => {
    const dataExport = {
      users,
      products,
      orders,
      categories,
      exportedAt: new Date().toISOString(),
      projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID
    };
    const jsonString = `data:text/json;charset=utf-8,${encodeURIComponent(JSON.stringify(dataExport, null, 2))}`;
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', jsonString);
    downloadAnchor.setAttribute('download', `bazaar-db-export-${Date.now()}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  const _importDatabaseFromJson = event => {
    if (useMockData) {
      alert('Please connect to your live Firebase project to import data.');
      return;
    }

    const fileReader = new FileReader();
    fileReader.onload = async e => {
      try {
        const importedData = JSON.parse(e.target.result);
        if (!importedData.users || !importedData.products || !importedData.orders) {
          throw new Error('Invalid backup file format. Must contain users, products, and orders.');
        }
        if (!window.confirm(`Found ${importedData.users.length} users, ${importedData.products.length} products, ${importedData.orders.length} orders, and ${(importedData.categories || []).length} categories. Import them into your database?`)) return;

        setLoading(true);
        setDbStatusMsg('Importing data. Please wait...');
        for (const u of importedData.users) if (u.email) await setDoc(doc(db, 'users', u.email), u);
        for (const p of importedData.products) if (p.id) await setDoc(doc(db, 'products', p.id.toString()), p);
        for (const o of importedData.orders) if (o.orderId) await setDoc(doc(db, 'orders', o.orderId), o);
        for (const c of importedData.categories || []) if (c.name) await setDoc(doc(db, 'categories', c.name), c);
        setDbStatusMsg('Database backup successfully restored.');
        setTimeout(() => setDbStatusMsg(''), 5000);
      } catch (err) {
        alert(`Error importing database backup: ${err.message}`);
      } finally {
        setLoading(false);
      }
    };

    if (event.target.files[0]) fileReader.readAsText(event.target.files[0]);
  };

  const patchUser = async (email, payload) => {
    if (useMockData) {
      setUsers(users.map(u => (u.email === email ? { ...u, ...payload } : u)));
      return;
    }
    try {
      await updateDoc(doc(db, 'users', email), payload);
    } catch (e) {
      alert(`Error updating user: ${e.message}`);
    }
  };

  const approveProfileEditRequest = async (email, user) => {
    const payload = {
      name: user.requestedName || user.name,
      shopName: user.requestedShopName || user.shopName,
      shopAddress: user.requestedShopAddress || user.shopAddress,
      shopAddressLat: user.requestedShopAddress ? user.requestedShopAddressLat : user.shopAddressLat || 0,
      shopAddressLng: user.requestedShopAddress ? user.requestedShopAddressLng : user.shopAddressLng || 0,
      deliveryMobile: user.requestedDeliveryMobile || user.deliveryMobile || "",
      deliveryVehicleType: user.requestedDeliveryVehicleType || user.deliveryVehicleType || "",
      deliveryVehicleNumber: user.requestedDeliveryVehicleNumber || user.deliveryVehicleNumber || "",
      deliveryEmergencyContact: user.requestedDeliveryEmergencyContact || user.deliveryEmergencyContact || "",
      deliveryAddress: user.requestedDeliveryAddress || user.deliveryAddress || "",
      deliveryAddressLat: user.requestedDeliveryAddress ? user.requestedDeliveryAddressLat : user.deliveryAddressLat || 0,
      deliveryAddressLng: user.requestedDeliveryAddress ? user.requestedDeliveryAddressLng : user.deliveryAddressLng || 0,

      editRequestPending: false,
      requestedName: "",
      requestedShopName: "",
      requestedShopAddress: "",
      requestedShopAddressLat: 0,
      requestedShopAddressLng: 0,
      requestedDeliveryMobile: "",
      requestedDeliveryVehicleType: "",
      requestedDeliveryVehicleNumber: "",
      requestedDeliveryEmergencyContact: "",
      requestedDeliveryAddress: "",
      requestedDeliveryAddressLat: 0,
      requestedDeliveryAddressLng: 0
    };
    await patchUser(email, payload);
  };

  const rejectProfileEditRequest = async (email) => {
    const payload = {
      editRequestPending: false,
      requestedName: "",
      requestedShopName: "",
      requestedShopAddress: "",
      requestedShopAddressLat: 0,
      requestedShopAddressLng: 0,
      requestedDeliveryMobile: "",
      requestedDeliveryVehicleType: "",
      requestedDeliveryVehicleNumber: "",
      requestedDeliveryEmergencyContact: "",
      requestedDeliveryAddress: "",
      requestedDeliveryAddressLat: 0,
      requestedDeliveryAddressLng: 0
    };
    await patchUser(email, payload);
  };

  const toggleSellerVerification = (email, currentStatus) => patchUser(email, {
    isSellerVerified: !currentStatus,
    isSellerVerificationPending: false
  });

  const toggleDeliveryVerification = (email, currentStatus) => patchUser(email, {
    isDeliveryPartnerVerified: !currentStatus
  });

  const deleteUser = async email => {
    if (!window.confirm(`Are you sure you want to delete user ${email}?`)) return;
    if (useMockData) {
      setUsers(users.filter(u => u.email !== email));
      return;
    }
    try {
      await deleteDoc(doc(db, 'users', email));
    } catch (e) {
      alert(`Error deleting user: ${e.message}`);
    }
  };

  const toggleProductFeatured = async (id, currentStatus) => {
    if (useMockData) {
      setProducts(products.map(p => (p.id === id ? { ...p, isFeatured: !currentStatus } : p)));
      return;
    }
    try {
      await updateDoc(doc(db, 'products', id.toString()), { isFeatured: !currentStatus });
    } catch (e) {
      alert(`Error updating product: ${e.message}`);
    }
  };

  const deleteProduct = async id => {
    if (!window.confirm('Are you sure you want to delete this product?')) return;
    if (useMockData) {
      setProducts(products.filter(p => p.id !== id));
      return;
    }
    try {
      await deleteDoc(doc(db, 'products', id.toString()));
    } catch (e) {
      alert(`Error deleting product: ${e.message}`);
    }
  };

  const handleCreateProduct = async e => {
    e.preventDefault();
    const priceNum = parseFloat(newProductPrice);
    const origPriceNum = parseFloat(newProductOrigPrice);
    if (!newProductName || Number.isNaN(priceNum) || Number.isNaN(origPriceNum)) {
      alert('Please fill in valid name and numeric prices.');
      return;
    }

    const maxId = products.reduce((max, p) => (Number(p.id) > max ? Number(p.id) : max), 0);
    const newId = maxId + 1;
    const newProd = {
      id: newId,
      name: newProductName,
      price: priceNum,
      originalPrice: origPriceNum,
      rating: 4.5,
      category: newProductCat || 'General',
      description: newProductDesc,
      imageUrlName: 'img_hero_banner',
      isFeatured: newProductFeatured,
      sellerEmail: newProductSeller || 'admin@bazaar.com',
      extraImages: ''
    };

    if (useMockData) {
      setProducts([...products, newProd]);
      setShowProductModal(false);
      resetProductForm();
      return;
    }

    try {
      await setDoc(doc(db, 'products', newId.toString()), newProd);
      setShowProductModal(false);
      resetProductForm();
    } catch (e) {
      alert(`Error creating product: ${e.message}`);
    }
  };

  const resetProductForm = () => {
    setNewProductName('');
    setNewProductPrice('');
    setNewProductOrigPrice('');
    setNewProductCat('');
    setNewProductDesc('');
    setNewProductFeatured(false);
  };

  const updateOrder = async (orderId, payload) => {
    if (useMockData) {
      setOrders(orders.map(o => (o.orderId === orderId ? { ...o, ...payload } : o)));
      return;
    }
    try {
      await updateDoc(doc(db, 'orders', orderId), payload);
    } catch (e) {
      alert(`Error updating order: ${e.message}`);
    }
  };

  const updateReturnRequest = async (orderId, requestId, payload) => {
    const order = orders.find(o => o.orderId === orderId);
    if (!order) {
      alert('Order not found for this return request.');
      return;
    }

    const updatedRequests = parseReturnRequests(order.returnRequestsJson).map(request => {
      if (request.id !== requestId) return request;
      const next = { ...request, ...payload, updatedAt: Date.now() };
      if (payload.status === 'Approved' && !Number(next.approvedDate || 0)) {
        next.approvedDate = Date.now();
      }
      return next;
    });

    const nextJson = JSON.stringify(updatedRequests);
    if (useMockData) {
      setOrders(orders.map(o => (o.orderId === orderId ? { ...o, returnRequestsJson: nextJson } : o)));
      return;
    }

    try {
      await updateDoc(doc(db, 'orders', orderId), { returnRequestsJson: nextJson });
    } catch (e) {
      alert(`Error updating return request: ${e.message}`);
    }
  };

  const updateWithdrawalStatus = async (requestId, nextStatus) => {
    const payload = {
      status: nextStatus,
      updatedAt: Date.now()
    };

    if (nextStatus === 'Pending') {
      payload.failureReason = '';
    }
    if (nextStatus === 'Paid') {
      payload.paidAt = Date.now();
      payload.failureReason = '';
    }
    if (nextStatus === 'Failed') {
      payload.failureReason = 'Rejected by admin';
    }

    if (useMockData) {
      setWithdrawalRequests(withdrawalRequests.map(req => (req.id === requestId ? { ...req, ...payload } : req)));
      return;
    }

    try {
      await updateDoc(doc(db, 'withdrawal_requests', requestId), payload);
    } catch (e) {
      alert(`Error updating withdrawal request: ${e.message}`);
    }
  };

  const toggleCouponActive = async (code, currentStatus) => {
    if (useMockData) {
      setCoupons(coupons.map(c => (c.code === code ? { ...c, isActive: !currentStatus } : c)));
      return;
    }
    try {
      await updateDoc(doc(db, 'coupons', code), { isActive: !currentStatus });
    } catch (e) {
      alert(`Error updating coupon: ${e.message}`);
    }
  };

  const deleteCoupon = async code => {
    if (!window.confirm(`Are you sure you want to delete coupon ${code}?`)) return;
    if (useMockData) {
      setCoupons(coupons.filter(c => c.code !== code));
      return;
    }
    try {
      await deleteDoc(doc(db, 'coupons', code));
    } catch (e) {
      alert(`Error deleting coupon: ${e.message}`);
    }
  };

  const handleCreateCoupon = async e => {
    e.preventDefault();
    const discountPercent = parseInt(newCouponDiscount, 10);
    const minOrderAmount = parseFloat(newCouponMinOrder || 0);
    const maxDiscount = parseFloat(newCouponMaxDiscount || 999999);
    if (!newCouponCode.trim() || Number.isNaN(discountPercent) || discountPercent <= 0 || discountPercent > 100) {
      alert('Please fill in a valid code and discount percent (1-100).');
      return;
    }

    const codeUpper = newCouponCode.trim().toUpperCase();

    const newCoup = {
      code: codeUpper,
      discountPercent,
      description: newCouponDesc,
      isActive: newCouponActive,
      minOrderAmount,
      maxDiscount
    };

    if (useMockData) {
      setCoupons([...coupons, newCoup]);
      setShowCouponModal(false);
      resetCouponForm();
      return;
    }

    try {
      await setDoc(doc(db, 'coupons', codeUpper), newCoup);
      setShowCouponModal(false);
      resetCouponForm();
    } catch (e) {
      alert(`Error creating coupon: ${e.message}`);
    }
  };

  const resetCouponForm = () => {
    setNewCouponCode('');
    setNewCouponDiscount('');
    setNewCouponMinOrder('');
    setNewCouponMaxDiscount('');
    setNewCouponDesc('');
    setNewCouponActive(true);
  };

  const saveServiceConfig = async event => {
    event.preventDefault();
    const payload = {
      ...appConfig,
      serviceCities: serviceCitiesText.split(',').map(v => v.trim()).filter(Boolean),
      servicePincodes: servicePincodesText.split(',').map(v => v.trim()).filter(Boolean),
      payoutDelayHours: Number(payoutDelayHours || 0)
    };
    if (useMockData) {
      setAppConfig(payload);
      setDbStatusMsg('Settings saved in demo mode.');
      setTimeout(() => setDbStatusMsg(''), 3000);
      return;
    }
    try {
      await setDoc(doc(db, 'app_config', 'main'), payload, { merge: true });
      setDbStatusMsg('Settings saved.');
      setTimeout(() => setDbStatusMsg(''), 3000);
    } catch (e) {
      alert(`Error saving settings: ${e.message}`);
    }
  };

  const handleCreateCategory = async event => {
    event.preventDefault();
    const name = newCategoryName.trim();
    if (!name) return;
    const isReturnable = name.toLowerCase() === 'food' || name.toLowerCase() === 'vegetables'
      ? false
      : newCategoryReturnable;
    const payload = { name, isReturnable };
    if (useMockData) {
      setCategories([...categories.filter(c => c.name !== name), payload].sort((a, b) => a.name.localeCompare(b.name)));
    } else {
      await setDoc(doc(db, 'categories', name), payload);
    }
    setNewCategoryName('');
    setNewCategoryReturnable(true);
  };

  const toggleCategoryReturnable = async category => {
    const forcedNonReturnable = ['food', 'vegetables'].includes(category.name.toLowerCase());
    const nextValue = forcedNonReturnable ? false : !category.isReturnable;
    if (useMockData) {
      setCategories(categories.map(c => c.name === category.name ? { ...c, isReturnable: nextValue } : c));
    } else {
      await updateDoc(doc(db, 'categories', category.name), { isReturnable: nextValue });
    }
  };

  const deleteCategory = async name => {
    if (!window.confirm(`Delete category "${name}"?`)) return;
    if (useMockData) {
      setCategories(categories.filter(c => c.name !== name));
    } else {
      await deleteDoc(doc(db, 'categories', name));
    }
  };

  if (authLoading) {
    return (
      <div className="auth-screen">
        <div className="empty-state">
          <RefreshCw className="empty-state-icon" style={{ animation: 'spin 1.5s linear infinite' }} size={48} />
          <h3>Checking admin session...</h3>
        </div>
      </div>
    );
  }

  if (!authUser) {
    return (
      <div className="auth-screen">
        <section className="glass-panel auth-card">
          <div className="logo-container auth-logo">
            <div className="logo-icon">
              <ShieldCheck size={22} color="#ffffff" />
            </div>
            <span className="logo-text">Bazaar Admin</span>
          </div>
          <h1>Admin Login</h1>
          <p className="auth-copy">Login with an app user from Firestore `users` whose role is `Admin`.</p>
          <form onSubmit={handleLogin} className="auth-form">
            <div className="form-group">
              <label className="form-label">Admin Email</label>
              <input className="form-control" type="email" value={loginEmail} onChange={e => setLoginEmail(e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">Password</label>
              <input className="form-control" type="password" value={loginPassword} onChange={e => setLoginPassword(e.target.value)} required />
            </div>
            {loginError && (
              <div className="inline-alert">
                <AlertTriangle size={16} />
                {loginError}
              </div>
            )}
            <button className="btn btn-primary" type="submit" disabled={loginSubmitting}>
              {loginSubmitting ? <RefreshCw size={16} style={{ animation: 'spin 1.5s linear infinite' }} /> : <KeyRound size={16} />}
              Login
            </button>
            <button className="btn btn-secondary" type="button" onClick={handleDemoLogin} style={{ marginTop: '8px', width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
              <Sparkles size={16} /> Run in Demo Mode (Local Mock)
            </button>
          </form>
        </section>
      </div>
    );
  }

  return (
    <div className="admin-layout">
      <aside className="sidebar">
        <div className="logo-container">
          <div className="logo-icon">
            <Sparkles size={22} color="#ffffff" />
          </div>
          <span className="logo-text">Bazaar Admin</span>
        </div>

        <nav className="sidebar-nav">
          {[
            ['dashboard', LayoutDashboard, 'Dashboard'],
            ['users', UsersIcon, 'Users'],
            ['products', ShoppingBag, 'Products'],
            ['orders', FileText, 'Orders'],
            ['returns', RefreshCw, 'Returns'],
            ['payments', CreditCard, 'Payments'],
            ['categories', Settings, 'Categories'],
            ['coupons', Ticket, 'Coupons'],
            ['settings', Settings, 'Service Settings']
          ].map(([tab, Icon, label]) => (
            <button key={tab} className={`nav-item ${activeTab === tab ? 'active' : ''}`} onClick={() => setActiveTab(tab)}>
              <Icon size={20} />
              {label}
              {tab === 'users' && pendingSellersCount + pendingPartnersCount > 0 && <span className="nav-count">{pendingSellersCount + pendingPartnersCount}</span>}
              {tab === 'returns' && pendingReturnCount > 0 && <span className="nav-count">{pendingReturnCount}</span>}
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="admin-profile">
            <div className="avatar">AD</div>
            <div>
              <p>{authUser.email}</p>
              <span>{useMockData ? 'Demo administrator' : 'Firebase administrator'}</span>
            </div>
          </div>
          <button className="btn btn-secondary btn-sm full-button" onClick={handleLogout}>
            <LogOut size={14} />
            Logout
          </button>
          {dbError && (
            <button className="btn btn-secondary btn-sm full-button" onClick={() => window.location.reload()}>
              <RefreshCw size={12} />
              Retry Firebase
            </button>
          )}
        </div>
      </aside>

      <main className="main-content">
        {dbError && (
          <div className="warning-banner">
            <div>
              <AlertTriangle size={22} />
              <div>
                <p>Database Connection Notice</p>
                <span>{dbError}</span>
              </div>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={() => window.location.reload()}>Retry</button>
          </div>
        )}

        {dbStatusMsg && (
          <div className="success-banner">
            <Sparkles size={20} />
            {dbStatusMsg}
          </div>
        )}

        <header className="header-wrapper">
          <div className="header-title">
            <h1>
              {activeTab === 'dashboard' && 'Dashboard Overview'}
              {activeTab === 'users' && 'User & Account Management'}
              {activeTab === 'products' && 'Inventory Products Catalog'}
              {activeTab === 'orders' && 'Order Transactions'}
              {activeTab === 'returns' && 'Return Requests'}
              {activeTab === 'payments' && 'Payment & Payout Ledger'}
              {activeTab === 'categories' && 'Category Return Rules'}
              {activeTab === 'coupons' && 'Coupon Directory'}
              {activeTab === 'settings' && 'Service Area & Payout Settings'}
            </h1 >
            <p>
              {activeTab === 'dashboard' && 'Monitor marketplace stats, verification queues, revenue, and data tools.'}
              {activeTab === 'users' && 'Review buyers, sellers, delivery partners, admin accounts, and verification documents.'}
              {activeTab === 'products' && 'Add new items, manage featured products, and review seller inventory.'}
              {activeTab === 'orders' && 'Update workflow statuses, delivery assignment, payment details, and order flags.'}
              {activeTab === 'returns' && 'Review customer return photos, seller approval status, delivery pickup assignment, and payout deductions.'}
              {activeTab === 'payments' && 'Track customer payments, seller receivables, product splits, and delivery partner earnings.'}
              {activeTab === 'categories' && 'Add seller listing categories and define whether customers can request returns.'}
              {activeTab === 'coupons' && 'Create, configure, and monitor discount promo coupons.'}
              {activeTab === 'settings' && 'Control eligible cities, pincodes, and automatic Razorpay payout timing.'}
            </p >
          </div >
        </header >

        {
          loading ? (
            <div className="empty-state" >
              <RefreshCw className="empty-state-icon" style={{ animation: 'spin 1.5s linear infinite' }} size={48} />
              <h3>Syncing with Firebase...</h3>
            </div>
          ) : (
            <>
              {activeTab === 'dashboard' && (
                <div>
                  <div className="metrics-grid">
                    <Metric title="Total Revenue" value={formatCurrency(totalRevenue)} icon={CheckCircle} />
                    <Metric title="Users" value={users.length} icon={UsersIcon} />
                    <Metric title="Products" value={products.length} icon={ShoppingBag} />
                    <Metric title="Orders" value={orders.length} icon={FileText} />
                  </div>

                  <div className="mini-metrics-grid">
                    <Metric title="Buyers" value={buyerCount} icon={UsersIcon} compact />
                    <Metric title="Sellers" value={`${activeSellersCount}/${sellerCount}`} icon={ShieldCheck} compact />
                    <Metric title="Delivery Partners" value={partnerCount} icon={Clock} compact />
                    <Metric title="Pending Reviews" value={pendingSellersCount + pendingPartnersCount} icon={AlertTriangle} compact />
                  </div>

                  {/*
                <div className="glass-panel section-card data-tools">
                  <div className="section-header">
                    <h2><Database size={20} /> Firebase Database Tools</h2>
                  </div>
                  <p>Seed Firestore with app-ready sample data, export a JSON backup, or restore a previous backup.</p>
                  <div className="toolbar-row">
                    <button className="btn btn-primary" onClick={seedFirestoreDatabase} disabled={useMockData}>
                      <Sparkles size={16} /> Seed Default Data
                    </button>
                    <button className="btn btn-secondary" onClick={exportDatabaseToJson}>
                      <Download size={16} /> Export JSON
                    </button>
                    <button className="btn btn-secondary" onClick={() => fileInputRef.current.click()} disabled={useMockData}>
                      <Upload size={16} /> Import JSON
                    </button>
                    <input type="file" ref={fileInputRef} onChange={importDatabaseFromJson} accept=".json" hidden />
                  </div>
                </div>
                */}

                  <div className="dashboard-grid">
                    <RecentOrders orders={orders} setActiveTab={setActiveTab} />
                    <div className="glass-panel section-card">
                      <div className="section-header">
                        <h2>Verification Queue</h2>
                      </div>
                      <QueueLine label="Sellers awaiting approval" value={pendingSellersCount} onClick={() => setActiveTab('users')} />
                      <QueueLine label="Delivery partners awaiting approval" value={pendingPartnersCount} onClick={() => setActiveTab('users')} />
                    </div>
                  </div>
                </div>
              )}

              {activeTab === 'users' && (
                <div className="glass-panel section-card">
                  <div className="section-header stacked-section-header">
                    <h2>Platform Accounts</h2>
                    <div className="toolbar-row">
                      <div className="search-box">
                        <Search size={16} />
                        <input value={userSearch} onChange={e => setUserSearch(e.target.value)} placeholder="Search accounts" />
                      </div>
                      <select className="form-control compact-select" value={userRoleFilter} onChange={e => setUserRoleFilter(e.target.value)}>
                        {ROLE_FILTERS.map(role => <option key={role} value={role}>{role === 'All' ? 'All roles' : roleLabel(role)}</option>)}
                      </select>
                    </div>
                  </div>
                  {filteredUsers.length === 0 ? (
                    <Empty icon={UsersIcon} title="No Users Found" text="No account matches the current filters." />
                  ) : (
                    <div className="table-container">
                      <table className="modern-table">
                        <thead>
                          <tr>
                            <th>User Profile</th>
                            <th>Role</th>
                            <th>Account Details</th>
                            <th>Verification</th>
                            <th>Manage</th>
                          </tr>
                        </thead>
                        <tbody>
                          {filteredUsers.map(user => (
                            <React.Fragment key={user.email}>
                              <tr>
                                <td>
                                  <div className="identity-cell">
                                    <div className="avatar">{(user.name || user.email || 'U').substring(0, 2).toUpperCase()}</div>
                                    <div>
                                      <p>{user.name || 'N/A'}</p>
                                      <span>{user.email}</span>
                                    </div>
                                  </div>
                                </td>
                                <td><span className={`role-chip role-${(user.role || 'User').toLowerCase()}`}>{roleLabel(user.role)}</span></td>
                                <td className="muted-cell">
                                  {user.role === 'Seller' && <><DetailLine label="Shop" value={user.shopName || 'N/A'} /><DetailLine label="Mobile" value={user.sellerMobile} /></>}
                                  {user.role === 'DeliveryPartner' && <><DetailLine label="Vehicle" value={`${user.deliveryVehicleType || 'Vehicle'} ${user.deliveryVehicleNumber || ''}`} /><DetailLine label="Mobile" value={user.deliveryMobile} /></>}
                                  {(!user.role || user.role === 'User' || user.role === 'Admin') && <><DetailLine label="Phone" value={user.phone} /><DetailLine label="Address" value={user.savedAddress || 'None configured'} /></>}
                                </td>
                                <td><UserStatus user={user} /></td>
                                <td>
                                  <div className="actions-row">
                                    {user.role === 'Seller' && <button className={`btn btn-sm ${user.isSellerVerified ? 'btn-secondary' : 'btn-primary'}`} onClick={() => toggleSellerVerification(user.email, user.isSellerVerified)}>{user.isSellerVerified ? 'Revoke' : 'Approve'}</button>}
                                    {user.role === 'DeliveryPartner' && <button className={`btn btn-sm ${user.isDeliveryPartnerVerified ? 'btn-secondary' : 'btn-primary'}`} onClick={() => toggleDeliveryVerification(user.email, user.isDeliveryPartnerVerified)}>{user.isDeliveryPartnerVerified ? 'Revoke' : 'Approve'}</button>}
                                    <button className="btn btn-secondary btn-sm" onClick={() => setExpandedUserEmail(expandedUserEmail === user.email ? '' : user.email)}>Details</button>
                                    <button className="btn btn-danger btn-sm" onClick={() => deleteUser(user.email)}><Trash2 size={14} /></button>
                                  </div>
                                </td>
                              </tr>
                              {expandedUserEmail === user.email && (
                                <tr className="detail-row">
                                  <td colSpan="5">
                                    <div className="detail-grid">
                                      <DetailLine label="Saved cards" value={user.savedCards} />
                                      <DetailLine label="Language" value={user.selectedLanguage} />
                                      <DetailLine label="Plus member" value={user.isPlusMember ? 'Yes' : 'No'} />
                                      <DetailLine label="Notifications" value={user.notificationsEnabled ? 'Enabled' : 'Disabled'} />
                                      <DetailLine label="Shop address" value={user.shopAddress} />
                                      <DetailLine label="Aadhaar" value={user.sellerAadhaar || user.deliveryAadhaar} />
                                      <DetailLine label="Bank account" value={user.sellerBankAccount || user.deliveryBankAccount} />
                                      <DetailLine label="PAN" value={user.sellerPanCard} />
                                      <DetailLine label="GST" value={user.sellerGstNumber} />
                                      <DetailLine label="Emergency contact" value={user.deliveryEmergencyContact} />
                                      <DetailLine label="Edit request" value={user.editRequestPending ? 'Pending Approval' : 'None'} />
                                    </div>

                                    {user.sellerVideoUrl && (
                                      <div style={{ marginTop: '16px' }}>
                                        <p style={{ margin: '0 0 6px 0', fontSize: '12px', color: '#666', fontWeight: 'bold' }}>Seller Introduction Video:</p>
                                        <video src={user.sellerVideoUrl} controls width="320" style={{ borderRadius: '8px', border: '1px solid #ddd' }} />
                                      </div>
                                    )}

                                    {(user.sellerShopPhoto || user.sellerOwnerPhoto || user.deliveryPhoto) && (
                                      <div style={{ marginTop: '16px', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
                                        {user.sellerShopPhoto && (
                                          <div>
                                            <p style={{ margin: '0 0 6px 0', fontSize: '12px', color: '#666', fontWeight: 'bold' }}>Shop Image:</p>
                                            <img src={user.sellerShopPhoto} alt="Shop" style={{ width: '120px', height: '120px', objectFit: 'cover', borderRadius: '8px', border: '1px solid #ddd' }} />
                                          </div>
                                        )}
                                        {user.sellerOwnerPhoto && (
                                          <div>
                                            <p style={{ margin: '0 0 6px 0', fontSize: '12px', color: '#666', fontWeight: 'bold' }}>Owner Selfie/Photo:</p>
                                            <img src={user.sellerOwnerPhoto} alt="Owner" style={{ width: '120px', height: '120px', objectFit: 'cover', borderRadius: '8px', border: '1px solid #ddd' }} />
                                          </div>
                                        )}
                                        {user.deliveryPhoto && (
                                          <div>
                                            <p style={{ margin: '0 0 6px 0', fontSize: '12px', color: '#666', fontWeight: 'bold' }}>Delivery Partner Photo:</p>
                                            <img src={user.deliveryPhoto} alt="Delivery Partner" style={{ width: '120px', height: '120px', objectFit: 'cover', borderRadius: '8px', border: '1px solid #ddd' }} />
                                          </div>
                                        )}
                                      </div>
                                    )}

                                    {user.editRequestPending && (
                                      <div className="edit-request-box" style={{
                                        marginTop: '16px',
                                        padding: '16px',
                                        borderRadius: '12px',
                                        background: 'rgba(232, 245, 233, 0.4)',
                                        border: '1px solid #c8e6c9',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        gap: '12px'
                                      }}>
                                        <h4 style={{ margin: 0, color: '#2e7d32', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                          ⏳ {user.role === 'Seller' ? 'Seller' : 'Delivery Partner'} Profile Edit Request Pending Approval
                                        </h4>
                                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                                          <div>
                                            <p style={{ margin: '0 0 4px 0', fontSize: '12px', color: '#666', fontWeight: 'bold' }}>Current Profile Info:</p>
                                            {user.role === 'Seller' ? (
                                              <div style={{ fontSize: '13px' }}>
                                                <div><strong>Owner Name:</strong> {user.name}</div>
                                                <div><strong>Shop Name:</strong> {user.shopName}</div>
                                                <div><strong>Shop Address:</strong> {user.shopAddress}</div>
                                              </div>
                                            ) : (
                                              <div style={{ fontSize: '13px' }}>
                                                <div><strong>Full Name:</strong> {user.name}</div>
                                                <div><strong>Mobile:</strong> {user.deliveryMobile}</div>
                                                <div><strong>Vehicle:</strong> {user.deliveryVehicleType} ({user.deliveryVehicleNumber})</div>
                                                <div><strong>Emergency Contact:</strong> {user.deliveryEmergencyContact}</div>
                                                <div><strong>Address:</strong> {user.deliveryAddress}</div>
                                              </div>
                                            )}
                                          </div>
                                          <div>
                                            <p style={{ margin: '0 0 4px 0', fontSize: '12px', color: '#2e7d32', fontWeight: 'bold' }}>Requested Profile Updates:</p>
                                            {user.role === 'Seller' ? (
                                              <div style={{ fontSize: '13px', color: '#1b5e20' }}>
                                                <div><strong>Owner Name:</strong> {user.requestedName || <span style={{ color: '#999', fontStyle: 'italic' }}>No change</span>}</div>
                                                <div><strong>Shop Name:</strong> {user.requestedShopName || <span style={{ color: '#999', fontStyle: 'italic' }}>No change</span>}</div>
                                                <div><strong>Shop Address:</strong> {user.requestedShopAddress || <span style={{ color: '#999', fontStyle: 'italic' }}>No change</span>}</div>
                                              </div>
                                            ) : (
                                              <div style={{ fontSize: '13px', color: '#1b5e20' }}>
                                                <div><strong>Full Name:</strong> {user.requestedName || <span style={{ color: '#999', fontStyle: 'italic' }}>No change</span>}</div>
                                                <div><strong>Mobile:</strong> {user.requestedDeliveryMobile || <span style={{ color: '#999', fontStyle: 'italic' }}>No change</span>}</div>
                                                <div><strong>Vehicle:</strong> {user.requestedDeliveryVehicleType ? `${user.requestedDeliveryVehicleType} (${user.requestedDeliveryVehicleNumber || 'N/A'})` : <span style={{ color: '#999', fontStyle: 'italic' }}>No change</span>}</div>
                                                <div><strong>Emergency Contact:</strong> {user.requestedDeliveryEmergencyContact || <span style={{ color: '#999', fontStyle: 'italic' }}>No change</span>}</div>
                                                <div><strong>Address:</strong> {user.requestedDeliveryAddress || <span style={{ color: '#999', fontStyle: 'italic' }}>No change</span>}</div>
                                              </div>
                                            )}
                                          </div>
                                        </div>
                                        <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
                                          <button className="btn btn-primary btn-sm" onClick={() => approveProfileEditRequest(user.email, user)}>
                                            Approve Profile Changes
                                          </button>
                                          <button className="btn btn-secondary btn-sm" onClick={() => rejectProfileEditRequest(user.email)}>
                                            Decline Profile Changes
                                          </button>
                                        </div>
                                      </div>
                                    )}
                                  </td>
                                </tr>
                              )}
                            </React.Fragment>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}

              {activeTab === 'products' && (
                <ProductsTab
                  products={products}
                  setShowProductModal={setShowProductModal}
                  toggleProductFeatured={toggleProductFeatured}
                  deleteProduct={deleteProduct}
                />
              )}

              {activeTab === 'orders' && (
                <div className="glass-panel section-card">
                  <div className="section-header stacked-section-header">
                    <h2>Order Ledger</h2>
                    <div className="toolbar-row">
                      <div className="search-box">
                        <Search size={16} />
                        <input value={orderSearch} onChange={e => setOrderSearch(e.target.value)} placeholder="Search orders" />
                      </div>
                      <select className="form-control compact-select" value={orderStatusFilter} onChange={e => setOrderStatusFilter(e.target.value)}>
                        <option value="All">All statuses</option>
                        {ORDER_STATUSES.map(status => <option key={status} value={status}>{status}</option>)}
                      </select>
                    </div>
                  </div>

                  {filteredOrders.length === 0 ? (
                    <Empty icon={FileText} title="No Orders Found" text="No order matches the current filters." />
                  ) : (
                    <div className="table-container">
                      <table className="modern-table">
                        <thead>
                          <tr>
                            <th>Order</th>
                            <th>Customer</th>
                            <th>Items</th>
                            <th>Payment</th>
                            <th>Status</th>
                            <th>Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {filteredOrders.map(order => (
                            <React.Fragment key={order.orderId}>
                              <tr>
                                <td>
                                  <p className="accent-text">{order.orderId}</p>
                                  <span className="table-subtext">{new Date(Number(order.orderDate || 0)).toLocaleString()}</span>
                                </td>
                                <td>
                                  <p>{order.email}</p>
                                  <span className="table-subtext clamp">{order.deliveryAddress || 'No delivery address'}</span>
                                </td>
                                <td>
                                  <p className="clamp">{order.itemsSummary}</p>
                                  {order.couponApplied && <span className="status-badge verified">{order.couponApplied}</span>}
                                </td>
                                <td>
                                  <p>{formatCurrency(order.totalAmount)}</p>
                                  <span className="table-subtext">{order.paymentMode || 'COD'}</span>
                                </td>
                                <td>
                                  <span className={`status-badge ${statusClass(order.status)}`}>{order.status || 'Pending'}</span>
                                </td>
                                <td>
                                  <div className="actions-row">
                                    <select className="form-control compact-select" value={order.status || 'Pending'} onChange={e => updateOrder(order.orderId, { status: e.target.value })}>
                                      {ORDER_STATUSES.map(status => <option key={status} value={status}>{status}</option>)}
                                    </select>
                                    <button className="btn btn-secondary btn-sm" onClick={() => setExpandedOrderId(expandedOrderId === order.orderId ? '' : order.orderId)}>Details</button>
                                  </div>
                                </td>
                              </tr>
                              {expandedOrderId === order.orderId && (
                                <tr className="detail-row">
                                  <td colSpan="6">
                                    <div className="detail-grid">
                                      <DetailLine label="Delivery partner" value={order.deliveryPartnerEmail || 'Not assigned'} />
                                      <DetailLine label="Delivery status" value={order.deliveryStatus || 'Not started'} />
                                      <DetailLine label="Seller confirmed" value={order.sellerConfirmed ? 'Yes' : 'No'} />
                                      <DetailLine label="Seller reject requested" value={order.sellerRejectRequested ? 'Yes' : 'No'} />
                                      <DetailLine label="Change delivery partner requested" value={order.sellerChangeDeliveryBoyRequested ? 'Yes' : 'No'} />
                                      <DetailLine label="Full address" value={order.deliveryAddress} />
                                    </div>
                                    <div className="toolbar-row detail-actions">
                                      <button className="btn btn-secondary btn-sm" onClick={() => updateOrder(order.orderId, { status: 'Shipped', deliveryStatus: 'On the Way' })}>Ship</button>
                                      <button className="btn btn-primary btn-sm" onClick={() => updateOrder(order.orderId, { status: 'Delivered', deliveryStatus: 'Delivered' })}>Deliver</button>
                                      <button className="btn btn-danger btn-sm" onClick={() => updateOrder(order.orderId, { status: 'Cancelled' })}>Cancel</button>
                                    </div>
                                  </td>
                                </tr>
                              )}
                            </React.Fragment>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}

              {
                activeTab === 'returns' && (
                  <div className="glass-panel section-card">
                    <div className="section-header stacked-section-header">
                      <h2>Return Request Ledger</h2>
                      <div className="toolbar-row">
                        <div className="search-box">
                          <Search size={16} />
                          <input value={returnSearch} onChange={e => setReturnSearch(e.target.value)} placeholder="Search returns" />
                        </div>
                        <select className="form-control compact-select" value={returnStatusFilter} onChange={e => setReturnStatusFilter(e.target.value)}>
                          <option value="All">All statuses</option>
                          {['Pending', 'Approved', 'Pickup Accepted', 'Completed', 'Rejected'].map(status => <option key={status} value={status}>{status}</option>)}
                        </select>
                      </div>
                    </div>

                    {filteredReturnRows.length === 0 ? (
                      <Empty icon={RefreshCw} title="No Return Requests" text="Customer return requests will appear here after users submit them from the app." />
                    ) : (
                      <div className="table-container settlement-table-container">
                        <table className="modern-table return-table">
                          <thead>
                            <tr>
                              <th>Return</th>
                              <th>Customer & Product</th>
                              <th>Seller Debit</th>
                              <th>Delivery Pickup</th>
                              <th>Status</th>
                              <th>Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {filteredReturnRows.map(request => (
                              <tr key={`${request.orderId}-${request.id}`}>
                                <td>
                                  <p className="accent-text">{request.id || 'Return Request'}</p>
                                  <span className="table-subtext">Order: {request.orderId}</span>
                                  <span className="table-subtext block-text">{request.requestDate ? new Date(Number(request.requestDate)).toLocaleString() : 'No request date'}</span>
                                  {request.photoUrl && (
                                    <a className="table-subtext block-text" href={request.photoUrl} target="_blank" rel="noreferrer">View customer photo</a>
                                  )}
                                </td>
                                <td>
                                  <p>{request.productName}</p>
                                  <span className="table-subtext">Customer: {request.customerName}</span>
                                  <span className="table-subtext block-text">{request.customerEmail}</span>
                                  <span className="table-subtext block-text">Reason: {request.reason || 'No reason provided'}</span>
                                </td>
                                <td>
                                  <p>{request.sellerName}</p>
                                  <span className="table-subtext block-text">{request.sellerEmail || 'System Store'}</span>
                                  <span className="table-subtext">Refund: {formatCurrency(request.returnAmount)}</span>
                                  <span className="table-subtext block-text">Return delivery: {formatCurrency(request.deliveryFee)}</span>
                                  <span className="table-subtext block-text payout-amount">Total debit: {formatCurrency(request.debitAmount)}</span>
                                </td>
                                <td>
                                  <p>{request.deliveryPartnerName}</p>
                                  <span className="table-subtext block-text">{request.deliveryPartnerEmail || 'Delivery boy not accepted yet'}</span>
                                  <span className="table-subtext block-text">Pickup: {request.order.deliveryAddress || 'Customer address missing'}</span>
                                </td>
                                <td>
                                  <span className={`status-badge ${statusClass(request.status)}`}>{request.status || 'Pending'}</span>
                                  {request.approvedDate > 0 && <span className="table-subtext block-text">Approved: {new Date(Number(request.approvedDate)).toLocaleString()}</span>}
                                </td>
                                <td>
                                  <div className="actions-row">
                                    <button className="btn btn-primary btn-sm" onClick={() => updateReturnRequest(request.orderId, request.id, { status: 'Approved' })}>
                                      Approve
                                    </button>
                                    <button className="btn btn-secondary btn-sm" onClick={() => updateReturnRequest(request.orderId, request.id, { status: 'Pending', deliveryPartnerEmail: '' })}>
                                      Pending
                                    </button>
                                    <button className="btn btn-secondary btn-sm" onClick={() => updateReturnRequest(request.orderId, request.id, { status: 'Completed' })}>
                                      Complete
                                    </button>
                                    <button className="btn btn-danger btn-sm" onClick={() => updateReturnRequest(request.orderId, request.id, { status: 'Rejected', deliveryPartnerEmail: '' })}>
                                      Reject
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )
              }

              {
                activeTab === 'payments' && (
                  <PaymentsTab
                    entries={settlementEntries}
                    totals={settlementTotals}
                    payoutAccounts={payoutAccounts}
                    withdrawalRequests={withdrawalRequests}
                    onUpdateWithdrawalStatus={updateWithdrawalStatus}
                    users={users}
                  />
                )
              }

              {
                activeTab === 'categories' && (
                  <div className="glass-panel section-card">
                    <div className="section-header stacked-section-header">
                      <h2>Category Return Controls</h2>
                    </div>

                    <form className="form-grid" onSubmit={handleCreateCategory}>
                      <div className="form-group">
                        <label className="form-label">Category Name</label>
                        <input
                          className="form-control"
                          value={newCategoryName}
                          onChange={e => setNewCategoryName(e.target.value)}
                          placeholder="e.g. Electronics, Food, Vegetables"
                          required
                        />
                      </div>
                      <div className="form-group checkbox-row">
                        <input
                          type="checkbox"
                          id="category-returnable"
                          checked={newCategoryReturnable}
                          onChange={e => setNewCategoryReturnable(e.target.checked)}
                          disabled={['food', 'vegetables'].includes(newCategoryName.trim().toLowerCase())}
                        />
                        <label htmlFor="category-returnable">
                          Returnable category
                        </label>
                      </div>
                      <div className="form-actions">
                        <button className="btn btn-primary" type="submit">
                          <Plus size={16} /> Save Category
                        </button>
                      </div>
                    </form>

                    {categories.length === 0 ? (
                      <Empty icon={Settings} title="No Categories Found" text="Add categories so sellers can select them during product listing." />
                    ) : (
                      <div className="table-container">
                        <table className="modern-table">
                          <thead>
                            <tr>
                              <th>Category</th>
                              <th>Return Rule</th>
                              <th>Manage</th>
                            </tr>
                          </thead>
                          <tbody>
                            {categories.map(category => {
                              const locked = ['food', 'vegetables'].includes(category.name.toLowerCase());
                              return (
                                <tr key={category.name}>
                                  <td>
                                    <p className="accent-text">{category.name}</p>
                                    {locked && <span className="table-subtext">Always non-returnable</span>}
                                  </td>
                                  <td>
                                    <span className={`status-badge ${category.isReturnable ? 'verified' : 'rejected'}`}>
                                      {category.isReturnable ? 'Returnable' : 'Non-returnable'}
                                    </span>
                                  </td>
                                  <td>
                                    <div className="actions-row">
                                      <button className="btn btn-secondary btn-sm" onClick={() => toggleCategoryReturnable(category)} disabled={locked}>
                                        {category.isReturnable ? 'Mark Non-returnable' : 'Mark Returnable'}
                                      </button>
                                      <button className="btn btn-danger btn-sm" onClick={() => deleteCategory(category.name)}>
                                        <Trash2 size={14} />
                                      </button>
                                    </div>
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )
              }

              {
                activeTab === 'coupons' && (
                  <div className="glass-panel section-card">
                    <div className="section-header stacked-section-header">
                      <h2>Coupon Directory</h2>
                      <div className="toolbar-row">
                        <button className="btn btn-primary" onClick={() => setShowCouponModal(true)}>
                          <Plus size={16} /> Create Coupon
                        </button>
                      </div>
                    </div>

                    {coupons.length === 0 ? (
                      <Empty icon={Ticket} title="No Coupons Found" text="Create coupons to offer discounts on checkout." />
                    ) : (
                      <div className="table-container">
                        <table className="modern-table">
                          <thead>
                            <tr>
                              <th>Code</th>
                              <th>Discount</th>
                              <th>Min Order</th>
                              <th>Max Discount</th>
                              <th>Description</th>
                              <th>Status</th>
                              <th>Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {coupons.map(coupon => (
                              <tr key={coupon.code}>
                                <td>
                                  <p className="accent-text" style={{ fontWeight: 'bold' }}>{coupon.code}</p>
                                </td>
                                <td>
                                  <p>{coupon.discountPercent}% OFF</p>
                                </td>
                                <td>
                                  <p>₹{coupon.minOrderAmount || 0}</p>
                                </td>
                                <td>
                                  <p>{coupon.maxDiscount && coupon.maxDiscount < 999999 ? `₹${coupon.maxDiscount}` : 'Unlimited'}</p>
                                </td>
                                <td className="muted-cell">
                                  <p>{coupon.description || 'No description provided'}</p>
                                </td>
                                <td>
                                  <span className={`status-badge ${coupon.isActive ? 'verified' : 'rejected'}`}>
                                    {coupon.isActive ? 'Active' : 'Inactive'}
                                  </span>
                                </td>
                                <td>
                                  <div className="actions-row">
                                    <button className={`btn btn-sm ${coupon.isActive ? 'btn-secondary' : 'btn-primary'}`} onClick={() => toggleCouponActive(coupon.code, coupon.isActive)}>
                                      {coupon.isActive ? 'Deactivate' : 'Activate'}
                                    </button>
                                    <button className="btn btn-danger btn-sm" onClick={() => deleteCoupon(coupon.code)}>
                                      <Trash2 size={14} />
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )
              }

              {
                activeTab === 'settings' && (
                  <div className="glass-panel section-card">
                    <div className="section-header"><h2>Fulfilment Configuration</h2></div>
                    <form className="form-grid" onSubmit={saveServiceConfig}>
                      <div className="form-group full-width">
                        <label className="form-label">Service Cities (comma-separated)</label>
                        <input className="form-control" value={serviceCitiesText} onChange={e => setServiceCitiesText(e.target.value)} placeholder="Delhi, Noida" />
                      </div>
                      <div className="form-group full-width">
                        <label className="form-label">Service Pincodes (comma-separated)</label>
                        <input className="form-control" value={servicePincodesText} onChange={e => setServicePincodesText(e.target.value)} placeholder="110001, 201301" />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Automatic payout delay (hours)</label>
                        <input type="number" min="0" className="form-control" value={payoutDelayHours} onChange={e => setPayoutDelayHours(e.target.value)} required />
                      </div>
                      <div className="form-actions"><button className="btn btn-primary" type="submit">Save Settings</button></div>
                    </form>
                  </div>
                )
              }
            </>
          )
        }
      </main >

      {showProductModal && (
        <div className="modal-overlay">
          <div className="glass-panel modal-content">
            <button className="close-btn" onClick={() => setShowProductModal(false)}>x</button>
            <h2 className="modal-title"><Plus size={22} color="var(--color-accent)" /> Publish New Product</h2>
            <form onSubmit={handleCreateProduct} className="form-grid">
              <div className="form-group full-width">
                <label className="form-label">Product Name</label>
                <input type="text" className="form-control" value={newProductName} onChange={e => setNewProductName(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Sale Price (₹)</label>
                <input type="number" step="0.01" className="form-control" value={newProductPrice} onChange={e => setNewProductPrice(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Original Price (₹)</label>
                <input type="number" step="0.01" className="form-control" value={newProductOrigPrice} onChange={e => setNewProductOrigPrice(e.target.value)} required />
              </div>
              <div className="form-group">
                <label className="form-label">Category</label>
                <select className="form-control" value={newProductCat} onChange={e => setNewProductCat(e.target.value)} required>
                  <option value="">Select category</option>
                  {categories.map(category => (
                    <option key={category.name} value={category.name}>
                      {category.name} ({category.isReturnable ? 'Returnable' : 'Non-returnable'})
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Seller Email Owner</label>
                <input type="email" className="form-control" value={newProductSeller} onChange={e => setNewProductSeller(e.target.value)} />
              </div>
              <div className="form-group full-width">
                <label className="form-label">Product Description</label>
                <textarea className="form-control" value={newProductDesc} onChange={e => setNewProductDesc(e.target.value)} />
              </div>
              <div className="form-group full-width checkbox-row">
                <input type="checkbox" id="featured-check" checked={newProductFeatured} onChange={e => setNewProductFeatured(e.target.checked)} />
                <label htmlFor="featured-check">Mark as Featured Showcase Product</label>
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowProductModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Publish Item</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {
        showCouponModal && (
          <div className="modal-overlay">
            <div className="glass-panel modal-content">
              <button className="close-btn" onClick={() => setShowCouponModal(false)}>x</button>
              <h2 className="modal-title"><Plus size={22} color="var(--color-accent)" /> Create New Coupon</h2>
              <form onSubmit={handleCreateCoupon} className="form-grid">
                <div className="form-group">
                  <label className="form-label">Coupon Code</label>
                  <input type="text" className="form-control" value={newCouponCode} onChange={e => setNewCouponCode(e.target.value)} placeholder="e.g. BAZAAR50" required />
                </div>
                <div className="form-group">
                  <label className="form-label">Discount Percent (1-100)</label>
                  <input type="number" min="1" max="100" className="form-control" value={newCouponDiscount} onChange={e => setNewCouponDiscount(e.target.value)} required />
                </div>
                <div className="form-group">
                  <label className="form-label">Min Order Amount (₹)</label>
                  <input type="number" min="0" step="0.01" className="form-control" value={newCouponMinOrder} onChange={e => setNewCouponMinOrder(e.target.value)} placeholder="0" />
                </div>
                <div className="form-group">
                  <label className="form-label">Max Discount Amount (₹)</label>
                  <input type="number" min="0" step="0.01" className="form-control" value={newCouponMaxDiscount} onChange={e => setNewCouponMaxDiscount(e.target.value)} placeholder="Unlimited" />
                </div>
                <div className="form-group full-width">
                  <label className="form-label">Description</label>
                  <input type="text" className="form-control" value={newCouponDesc} onChange={e => setNewCouponDesc(e.target.value)} placeholder="Description of the coupon" />
                </div>
                <div className="form-group full-width checkbox-row">
                  <input type="checkbox" id="coupon-active-check" checked={newCouponActive} onChange={e => setNewCouponActive(e.target.checked)} />
                  <label htmlFor="coupon-active-check">Mark as Active instantly</label>
                </div>
                <div className="form-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => setShowCouponModal(false)}>Cancel</button>
                  <button type="submit" className="btn btn-primary">Create Coupon</button>
                </div>
              </form>
            </div>
          </div>
        )
      }
    </div >
  );
}

function Metric({ title, value, icon: Icon, compact = false }) {
  return (
    <div className={`glass-panel metric-card ${compact ? 'compact-metric' : ''}`}>
      <div className="metric-info">
        <h3>{title}</h3>
        <div className="value">{value}</div>
      </div>
      <div className="metric-icon-box">
        <Icon size={compact ? 18 : 24} />
      </div>
    </div>
  );
}

function Empty({ icon: Icon, title, text }) {
  return (
    <div className="empty-state">
      <Icon className="empty-state-icon" size={48} />
      <h3>{title}</h3>
      <p>{text}</p>
    </div>
  );
}

function QueueLine({ label, value, onClick }) {
  return (
    <div className="queue-line">
      <div>
        <p>{label}</p>
        <strong>{value}</strong>
      </div>
      <button className="btn btn-primary btn-sm" onClick={onClick}>Review</button>
    </div>
  );
}

function RecentOrders({ orders, setActiveTab }) {
  return (
    <div className="glass-panel section-card">
      <div className="section-header">
        <h2>Recent Orders</h2>
        <button className="btn btn-secondary btn-sm" onClick={() => setActiveTab('orders')}>View All</button>
      </div>
      {orders.length === 0 ? (
        <Empty icon={Clock} title="No orders placed yet" text="Orders from the app will appear here." />
      ) : (
        <div className="table-container">
          <table className="modern-table">
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Customer</th>
                <th>Amount</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {orders.slice(0, 5).map(order => (
                <tr key={order.orderId}>
                  <td className="accent-text">{order.orderId}</td>
                  <td>{order.email}</td>
                  <td>{formatCurrency(order.totalAmount)}</td>
                  <td><span className={`status-badge ${statusClass(order.status)}`}>{order.status || 'Pending'}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function UserStatus({ user }) {
  if (user.role === 'Seller') {
    return <span className={`status-badge ${user.isSellerVerified ? 'verified' : 'pending'}`}>{user.isSellerVerified ? 'Verified Seller' : 'Seller Review'}</span>;
  }
  if (user.role === 'DeliveryPartner') {
    return <span className={`status-badge ${user.isDeliveryPartnerVerified ? 'verified' : 'pending'}`}>{user.isDeliveryPartnerVerified ? 'Verified Partner' : 'Partner Review'}</span>;
  }
  return <span className="status-badge verified">Active Account</span>;
}

function ProductsTab({ products, setShowProductModal, toggleProductFeatured, deleteProduct }) {
  return (
    <div className="glass-panel section-card product-section-card">
      <div className="section-header">
        <h2>Products Collection</h2>
        <button className="btn btn-primary" onClick={() => setShowProductModal(true)}>
          <Plus size={18} /> Add New Product
        </button>
      </div>

      {products.length === 0 ? (
        <Empty icon={ShoppingBag} title="No Products Listed" text="Start listing products by tapping the button above." />
      ) : (
        <div className="table-container product-table-container">
          <table className="modern-table product-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Product Name</th>
                <th>Category</th>
                <th>Market Price</th>
                <th>Featured</th>
                <th>Seller Account</th>
                <th>Manage</th>
              </tr>
            </thead>
            <tbody>
              {products.map(product => (
                <tr key={product.id}>
                  <td className="table-subtext">#{product.id}</td>
                  <td>
                    <p>{product.name}</p>
                    <span className="table-subtext clamp">{product.description}</span>
                  </td>
                  <td>{product.category}</td>
                  <td>
                    <p className="accent-text">{formatCurrency(product.price)}</p>
                    <span className="table-subtext strike">{formatCurrency(product.originalPrice)}</span>
                  </td>
                  <td>
                    <button className="icon-button" onClick={() => toggleProductFeatured(product.id, product.isFeatured)} title="Toggle featured">
                      <Star size={20} fill={product.isFeatured ? '#eab308' : 'none'} />
                    </button>
                  </td>
                  <td className="table-subtext">{product.sellerEmail || 'System Store'}</td>
                  <td><button className="btn btn-danger btn-sm" onClick={() => deleteProduct(product.id)}><Trash2 size={14} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function PaymentsTab({ entries, totals, payoutAccounts, withdrawalRequests, onUpdateWithdrawalStatus, users }) {
  const [subTab, setSubTab] = useState('ledger');

  const returnRows = entries.flatMap(entry =>
    entry.returns.map(request => ({
      ...request,
      orderId: entry.order.orderId,
      buyerEmail: entry.buyerEmail,
      productName: request.productName || `Product #${request.productId || 'N/A'}`
    }))
  );

  return (
    <div>
      <div className="mini-metrics-grid">
        <Metric title="Customer Paid" value={formatCurrency(totals.paid)} icon={CreditCard} compact />
        <Metric title="Seller Receivable" value={formatCurrency(totals.sellers)} icon={ShoppingBag} compact />
        <Metric title="Delivery Receivable" value={formatCurrency(totals.delivery)} icon={Truck} compact />
        <Metric title="Return Debit" value={formatCurrency(totals.returnDebit)} icon={RefreshCw} compact />
        <Metric title="Payout Ready" value={formatCurrency(totals.payoutReady)} icon={CheckCircle} compact />
        <Metric title="Payout Pending" value={formatCurrency(totals.payoutPending)} icon={Clock} compact />
      </div>

      <div style={{ display: 'flex', gap: '10px', margin: '24px 0 16px 0', flexWrap: 'wrap' }}>
        {[
          { id: 'ledger', label: 'Payment Ledger', icon: CreditCard },
          { id: 'returns', label: 'Return Debits', icon: RefreshCw },
          { id: 'payouts', label: 'Payout Accounts', icon: CheckCircle },
          { id: 'requests', label: 'Withdrawal Requests', icon: Clock }
        ].map(tab => {
          const Icon = tab.icon;
          const active = subTab === tab.id;
          return (
            <button
              key={tab.id}
              className={`btn btn-sm ${active ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setSubTab(tab.id)}
              style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Icon size={14} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {subTab === 'ledger' && (
        <div className="glass-panel section-card">
          <div className="section-header stacked-section-header">
            <h2><CreditCard size={20} /> Payment Details</h2>
          </div>

          {entries.length === 0 ? (
            <Empty icon={CreditCard} title="No Payments Found" text="Customer payment and payout details will appear after orders are placed." />
          ) : (
            <div className="table-container settlement-table-container">
              <table className="modern-table settlement-table">
                <thead>
                  <tr>
                    <th>Order & User</th>
                    <th>Payment</th>
                    <th>Product Seller Split</th>
                    <th>Delivery Boy</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map(entry => (
                    <tr key={entry.order.orderId}>
                      <td>
                        <p className="accent-text">{entry.order.orderId}</p>
                        <span className="table-subtext">{entry.buyerName}</span>
                        <span className="table-subtext block-text">{entry.buyerEmail}</span>
                        <span className="table-subtext block-text">{new Date(Number(entry.order.orderDate || 0)).toLocaleString()}</span>
                      </td>
                      <td>
                        <p className="accent-text">{formatCurrency(entry.totalPaid)}</p>
                        <span className="table-subtext">{entry.order.paymentMode || 'COD'}</span>
                        {entry.order.paymentTransactionId && (
                          <span className="table-subtext block-text">Txn: {entry.order.paymentTransactionId}</span>
                        )}
                        {entry.order.couponApplied && <span className="status-badge verified">{entry.order.couponApplied}</span>}
                      </td>
                      <td>
                        <div className="settlement-lines">
                          {entry.items.length === 0 ? (
                            <span className="table-subtext">No product mapping found</span>
                          ) : (
                            entry.items.map((item, index) => (
                              <div className="settlement-line" key={`${entry.order.orderId}-${item.productId || item.productName}-${index}`}>
                                <div>
                                  <p>{item.productName}</p>
                                  <span className="table-subtext">
                                    Qty {item.quantity} x {formatCurrency(item.unitPrice)} | Seller: {item.sellerName}
                                  </span>
                                  {item.sellerEmail && <span className="table-subtext block-text">{item.sellerEmail}</span>}
                                </div>
                                <strong>{formatCurrency(item.sellerReceivable)}</strong>
                              </div>
                            ))
                          )}
                        </div>
                      </td>
                      <td>
                        {entry.deliveryPartnerEmail ? (
                          <>
                            <p>{entry.deliveryPartnerName}</p>
                            <span className="table-subtext">{entry.deliveryPartnerEmail}</span>
                            <p className="accent-text payout-amount">{formatCurrency(entry.deliveryEarning)}</p>
                          </>
                        ) : (
                          <>
                            <span className="status-badge pending">Not Accepted</span>
                            <p className="table-subtext payout-amount">{formatCurrency(entry.deliveryEarning)} pending</p>
                          </>
                        )}
                      </td>
                      <td>
                        <span className={`status-badge ${statusClass(entry.order.status)}`}>{entry.order.status || 'Pending'}</span>
                        <span className="table-subtext block-text">{entry.order.deliveryStatus || 'Delivery not started'}</span>
                        {isDeliveredOrder(entry.order) && (
                          <span className="table-subtext block-text">
                            Return window: {entry.returnWindowEnd ? new Date(entry.returnWindowEnd).toLocaleDateString() : 'N/A'}
                          </span>
                        )}
                        <span className={`status-badge ${entry.payoutReady ? 'verified' : 'pending'}`}>
                          {entry.payoutReady ? 'Withdraw Ready' : 'Withdraw Pending'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {subTab === 'returns' && (
        <div className="glass-panel section-card">
          <div className="section-header stacked-section-header">
            <h2><RefreshCw size={20} /> Return Debit Details</h2>
          </div>

          {returnRows.length === 0 ? (
            <Empty icon={RefreshCw} title="No Return Debits" text="Approved or completed return deductions will appear here." />
          ) : (
            <div className="table-container settlement-table-container">
              <table className="modern-table return-table">
                <thead>
                  <tr>
                    <th>Return</th>
                    <th>Seller Account</th>
                    <th>Debit Breakup</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {returnRows.map(request => (
                    <tr key={`${request.orderId}-${request.id}`}>
                      <td>
                        <p className="accent-text">{request.id || 'Return Request'}</p>
                        <span className="table-subtext">Order: {request.orderId}</span>
                        <span className="table-subtext block-text">Buyer: {request.buyerEmail}</span>
                        <span className="table-subtext block-text">{request.productName}</span>
                      </td>
                      <td>
                        <p>{request.sellerName}</p>
                        <span className="table-subtext">{request.sellerEmail || 'System Store'}</span>
                      </td>
                      <td>
                        <p className="accent-text">{formatCurrency(request.debitAmount)}</p>
                        <span className="table-subtext">Refund: {formatCurrency(request.returnAmount)}</span>
                        <span className="table-subtext block-text">Return delivery: {formatCurrency(request.deliveryFee)}</span>
                      </td>
                      <td>
                        <span className={`status-badge ${request.isCompleted ? 'cancelled' : 'pending'}`}>
                          {request.isCompleted ? 'Actual Debit' : 'Expected Debit'}
                        </span>
                        <span className="table-subtext block-text">{request.status || 'Pending'}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {subTab === 'payouts' && (
        <div className="glass-panel section-card">
          <div className="section-header stacked-section-header">
            <h2><CheckCircle size={20} /> Withdraw Pending / Ready</h2>
            <p className="section-note">Return window: {RETURN_WINDOW_DAYS} days after delivery. Ready amount can be paid to the listed bank account.</p>
          </div>

          {payoutAccounts.length === 0 ? (
            <Empty icon={CheckCircle} title="No Payout Accounts" text="Seller and delivery partner payout rows will appear after delivered orders." />
          ) : (
            <div className="table-container settlement-table-container">
              <table className="modern-table payout-table">
                <thead>
                  <tr>
                    <th>Account</th>
                    <th>Bank Details</th>
                    <th>Ready To Pay</th>
                    <th>Pending</th>
                    <th>Return Debits</th>
                    <th>Orders</th>
                  </tr>
                </thead>
                <tbody>
                  {payoutAccounts.map(account => (
                    <tr key={account.key}>
                      <td>
                        <p>{account.name}</p>
                        <span className={`role-chip role-${account.role.toLowerCase()}`}>{roleLabel(account.role)}</span>
                        <span className="table-subtext block-text">{account.email || 'System account'}</span>
                      </td>
                      <td className="muted-cell">
                        <p>{account.bankDetails}</p>
                      </td>
                      <td>
                        <p className="accent-text">{formatCurrency(account.readyAmount)}</p>
                        <span className="status-badge verified">Withdraw Ready</span>
                      </td>
                      <td>
                        <p>{formatCurrency(account.pendingAmount)}</p>
                        <span className="status-badge pending">Return Window Pending</span>
                      </td>
                      <td>
                        <p className="status-badge cancelled">{formatCurrency(account.returnDebits)} cut</p>
                        {account.expectedReturnDebits > 0 && (
                          <span className="table-subtext block-text">Expected: {formatCurrency(account.expectedReturnDebits)}</span>
                        )}
                      </td>
                      <td className="muted-cell">
                        <p>{account.orders.join(', ')}</p>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {subTab === 'requests' && (
        <div className="glass-panel section-card">
          <div className="section-header stacked-section-header">
            <h2><CreditCard size={20} /> Withdrawal Requests</h2>
            <p className="section-note">Admin can mark each request as Pending, Success, or Rejected.</p>
          </div>

          {withdrawalRequests.length === 0 ? (
            <Empty icon={CreditCard} title="No Withdrawal Requests" text="Seller and delivery partner withdrawal requests will appear here." />
          ) : (
            <div className="table-container settlement-table-container">
              <table className="modern-table payout-table">
                <thead>
                  <tr>
                    <th>Request</th>
                    <th>Account</th>
                    <th>Amount</th>
                    <th>Bank Details</th>
                    <th>Status</th>
                    <th>Admin Action</th>
                  </tr>
                </thead>
                <tbody>
                  {withdrawalRequests.map(request => {
                    const accountEmail = request.accountEmail || request.deliveryPartnerEmail || '';
                    const accountName = getDisplayNameByEmail(users, accountEmail, accountEmail || 'Account');

                    return (
                      <tr key={request.id}>
                        <td>
                          <p className="accent-text">{request.id}</p>
                          <span className="table-subtext">{new Date(Number(request.requestDate || 0)).toLocaleString()}</span>
                          {request.payoutId && <span className="table-subtext block-text">Payout: {request.payoutId}</span>}
                        </td>
                        <td>
                          <p>{accountName}</p>
                          <span className={`role-chip role-${(request.accountRole || 'DeliveryPartner').toLowerCase()}`}>{roleLabel(request.accountRole)}</span>
                          <span className="table-subtext block-text">{accountEmail}</span>
                        </td>
                        <td>
                          <p className="accent-text">{formatCurrency(request.amount)}</p>
                        </td>
                        <td className="muted-cell">
                          <p>{request.bankDetails || 'Bank details not available'}</p>
                          {request.failureReason && <span className="table-subtext block-text">Reason: {request.failureReason}</span>}
                        </td>
                        <td>
                          <span className={`status-badge ${withdrawalStatusClass(request.status)}`}>
                            {withdrawalStatusLabel(request.status)}
                          </span>
                          <span className="table-subtext block-text">{request.status || 'Scheduled'}</span>
                        </td>
                        <td>
                          <div className="actions-row">
                            {WITHDRAWAL_STATUS_ACTIONS.map(action => (
                              <button
                                key={action.status}
                                className={`btn btn-sm ${action.className === 'cancelled' ? 'btn-danger' : action.className === 'verified' ? 'btn-primary' : 'btn-secondary'}`}
                                onClick={() => onUpdateWithdrawalStatus(request.id, action.status)}
                                disabled={request.status === action.status}
                              >
                                {action.label}
                              </button>
                            ))}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default App;
