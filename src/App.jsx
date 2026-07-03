import React, { useEffect, useMemo, useState, useRef } from 'react';
import {
  AlertTriangle,
  CheckCircle,
  Clock,
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
  Users as UsersIcon,
  MapPin,
  Tag,
  Ban,
  UserX
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
import { db } from './firebase';

const ORDER_STATUSES = ['Pending', 'Processing', 'Ready to Deliver', 'Shipped', 'Delivered', 'Cancelled', 'Seller Reject Requested'];
const ROLE_FILTERS = ['All', 'User', 'Seller', 'DeliveryPartner', 'Admin'];

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
  { email: 'buyer@bazaar.com', name: 'Green Buyer', role: 'User', isPlusMember: true, savedAddress: '22 Market Street, Eco City', phone: '+91 8888811111', isDisabled: false },
  { email: 'seller@store.com', name: 'Organic Farms Co.', role: 'Seller', isSellerVerified: true, isSellerVerificationPending: false, shopName: 'Organic Farms', shopAddress: 'Green Valley Farm Road', sellerMobile: '+91 7777711111', sellerGstNumber: 'GST-ZVB-1001', sellerAadhaar: '1234-5678-9012', sellerPanCard: 'ABCDE1234F', sellerBankAccount: '98765432101', sellerIfsc: 'BARB0ECOCIT', bankName: 'Bank of Baroda', bankHolderName: 'Organic Farms Co.' },
  { email: 'pending.seller@store.com', name: 'Fresh Cart Seller', role: 'Seller', isSellerVerified: false, isSellerVerificationPending: true, shopName: 'Fresh Cart', shopAddress: 'Unit 8, Local Bazaar', sellerMobile: '+91 7777722222', sellerAadhaar: '9876-5432-1098', sellerPanCard: 'XYZWR9876A', sellerBankAccount: '112233445566', sellerIfsc: 'SBIN0001234', bankName: 'State Bank of India', bankHolderName: 'Fresh Cart Seller', editRequestPending: true, requestedName: 'Fresh Cart Super Store', requestedShopName: 'Fresh Cart Mega Mart' },
  { email: 'rider@bazaar.com', name: 'Fast Courier Rider', role: 'DeliveryPartner', isDeliveryPartnerVerified: true, deliveryMobile: '+91 9999911111', deliveryVehicleType: 'Electric Scooter', deliveryVehicleNumber: 'DL-3C-EC-9999', deliveryEmergencyContact: '+91 9999922222', deliveryBankAccount: '888877776666', deliveryIfsc: 'HDFC0000456', bankName: 'HDFC Bank', bankHolderName: 'Fast Courier Rider' },
  { email: 'pending.rider@bazaar.com', name: 'New Delivery Partner', role: 'DeliveryPartner', isDeliveryPartnerVerified: false, deliveryMobile: '+91 9999933333', deliveryVehicleType: 'Bike', deliveryVehicleNumber: 'DL-4S-NP-2211', deliveryBankAccount: '555566667777', deliveryIfsc: 'ICIC0000789', bankName: 'ICICI Bank', bankHolderName: 'New Delivery Partner' }
];

const defaultOrders = [
  { orderId: 'ORD-9872', email: 'buyer@bazaar.com', orderDate: Date.now() - 3600000 * 2, totalAmount: 134.98, status: 'Processing', itemsSummary: '1x ZYL Sound Pro Wireless ANC, 1x Organic Apples', paymentMode: 'Card ending in 4242', deliveryAddress: '22 Market Street, Eco City', couponApplied: 'GREEN10', deliveryPartnerEmail: 'rider@bazaar.com', deliveryStatus: 'Assigned', sellerConfirmed: true, sellerEmail: 'seller@store.com' },
  { orderId: 'ORD-5431', email: 'buyer@bazaar.com', orderDate: Date.now() - 3600000 * 24, totalAmount: 4.99, status: 'Delivered', itemsSummary: '1x Premium Farms Organic Apples (1kg)', paymentMode: 'Wallet', deliveryAddress: '22 Market Street, Eco City', deliveryPartnerEmail: 'rider@bazaar.com', deliveryStatus: 'Delivered', sellerConfirmed: true, sellerEmail: 'seller@store.com' }
];

const defaultAreas = [
  { id: 'area_1', city: 'Eco City', pinCode: '54002' },
  { id: 'area_2', city: 'Green Valley', pinCode: '56001' }
];

const defaultCoupons = [
  { id: 'coupon_1', code: 'GREEN10', amount: 10 },
  { id: 'coupon_2', code: 'BAZAAR50', amount: 50 }
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
  const [areas, setAreas] = useState([]);
  const [coupons, setCoupons] = useState([]);
  
  const [loading, setLoading] = useState(false);
  const [dbError, setDbError] = useState(null);
  const [useMockData, setUseMockData] = useState(false);
  const [dbStatusMsg, setDbStatusMsg] = useState('');

  const [userSearch, setUserSearch] = useState('');
  const [userRoleFilter, setUserRoleFilter] = useState('All');
  const [orderSearch, setOrderSearch] = useState('');
  const [orderStatusFilter, setOrderStatusFilter] = useState('All');
  const [expandedUserEmail, setExpandedUserEmail] = useState('');
  const [expandedOrderId, setExpandedOrderId] = useState('');

  // Service Area states
  const [newCityName, setNewCityName] = useState('');
  const [newPinCode, setNewPinCode] = useState('');

  // Coupons states
  const [newCouponCode, setNewCouponCode] = useState('');
  const [newCouponAmount, setNewCouponAmount] = useState('');

  const [showProductModal, setShowProductModal] = useState(false);
  const [newProductName, setNewProductName] = useState('');
  const [newProductPrice, setNewProductPrice] = useState('');
  const [newProductOrigPrice, setNewProductOrigPrice] = useState('');
  const [newProductCat, setNewProductCat] = useState('');
  const [newProductDesc, setNewProductDesc] = useState('');
  const [newProductSeller, setNewProductSeller] = useState('admin@bazaar.com');
  const [newProductFeatured, setNewProductFeatured] = useState(false);

  const fileInputRef = useRef(null);

  useEffect(() => {
    const savedSession = window.localStorage.getItem('bazaarAdminSession');
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
      setAreas([]);
      setCoupons([]);
      setLoading(false);
      return undefined;
    }

    if (useMockData) {
      setUsers(defaultUsers);
      setProducts(defaultProducts);
      setOrders(defaultOrders);
      setAreas(defaultAreas);
      setCoupons(defaultCoupons);
      setLoading(false);
      return undefined;
    }

    let unsubscribeUsers = () => { };
    let unsubscribeProducts = () => { };
    let unsubscribeOrders = () => { };
    let unsubscribeAreas = () => { };
    let unsubscribeCoupons = () => { };

    try {
      setLoading(true);
      unsubscribeUsers = onSnapshot(
        collection(db, 'users'),
        snapshot => {
          const list = snapshot.docs.map(userDoc => ({ ...userDoc.data(), docId: userDoc.id, email: userDoc.data().email || userDoc.id }));
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

      unsubscribeAreas = onSnapshot(
        collection(db, 'service_areas'),
        snapshot => {
          const list = snapshot.docs.map(areaDoc => ({ ...areaDoc.data(), id: areaDoc.id }));
          setAreas(list.sort((a, b) => (a.city || '').localeCompare(b.city || '') || (a.pinCode || '').localeCompare(b.pinCode || '')));
        },
        err => {
          console.warn('Firestore service areas sync failed:', err);
        }
      );

      unsubscribeCoupons = onSnapshot(
        collection(db, 'coupons'),
        snapshot => {
          const list = snapshot.docs.map(couponDoc => ({ ...couponDoc.data(), id: couponDoc.id }));
          setCoupons(list.sort((a, b) => (a.code || '').localeCompare(b.code || '')));
        },
        err => {
          console.warn('Firestore coupons sync failed:', err);
        }
      );
    } catch (error) {
      console.error('Firebase init failed, switching to mock:', error);
      setDbError('Invalid Firebase configuration.');
      setLoading(false);
    }

    return () => {
      unsubscribeUsers();
      unsubscribeProducts();
      unsubscribeOrders();
      unsubscribeAreas();
      unsubscribeCoupons();
    };
  }, [authUser, useMockData]);

  const hasEditRequest = (user) => {
    return !!(user.editRequestPending || 
              (user.editRequest && Object.keys(user.editRequest).length > 0) || 
              user.requestedName || 
              user.requestedShopName || 
              user.requestedShopAddress || 
              user.requestedSellerMobile || 
              user.requestedDeliveryMobile || 
              user.requestedDeliveryVehicleType || 
              user.requestedDeliveryVehicleNumber);
  };

  // Metric calculation variables
  const activeSellersCount = users.filter(u => u.role === 'Seller' && u.isSellerVerified).length;
  const pendingSellersCount = users.filter(u => u.role === 'Seller' && !u.isSellerVerified && !u.isSellerRejected).length;
  const pendingEditRequestsCount = users.filter(hasEditRequest).length;
  const pendingPartnersCount = users.filter(u => u.role === 'DeliveryPartner' && !u.isDeliveryPartnerVerified && !u.isDeliveryPartnerRejected).length;
  const buyerCount = users.filter(u => !u.role || u.role === 'User').length;
  const sellerCount = users.filter(u => u.role === 'Seller').length;
  const partnerCount = users.filter(u => u.role === 'DeliveryPartner').length;

  const totalRevenue = orders
    .filter(o => ['Delivered', 'Processing', 'Ready to Deliver', 'Shipped'].includes(o.status))
    .reduce((sum, o) => sum + Number(o.totalAmount || 0), 0);

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

  // Helper getters for order view
  const getOrderCustomerName = (order) => {
    if (order.userName) return order.userName;
    const found = users.find(u => u.email === order.email);
    return found?.name || 'Guest User';
  };

  const getOrderSeller = (order) => {
    if (order.sellerName) return order.sellerName;
    if (order.sellerEmail) {
      const found = users.find(u => u.email === order.sellerEmail);
      return found?.shopName || found?.name || order.sellerEmail;
    }
    if (order.itemsSummary) {
      const matchingProduct = products.find(p => order.itemsSummary.includes(p.name));
      if (matchingProduct && matchingProduct.sellerEmail) {
        const found = users.find(u => u.email === matchingProduct.sellerEmail);
        return found?.shopName || found?.name || matchingProduct.sellerEmail;
      }
    }
    return 'System Store';
  };

  const getOrderDeliveryPartner = (order) => {
    if (order.deliveryPartnerName) return order.deliveryPartnerName;
    if (order.deliveryPartnerEmail) {
      const found = users.find(u => u.email === order.deliveryPartnerEmail);
      return found?.name || order.deliveryPartnerEmail;
    }
    return 'Not Assigned';
  };

  // Helper getter for profile edit requests
  const getRequestedValue = (user, field, fallback) => {
    if (user.editRequest && user.editRequest[field] !== undefined && user.editRequest[field] !== null) {
      return user.editRequest[field];
    }
    const flatField = 'requested' + field.charAt(0).toUpperCase() + field.slice(1);
    if (user[flatField] !== undefined && user[flatField] !== null) {
      return user[flatField];
    }
    return fallback;
  };

  const handleLogin = async event => {
    event.preventDefault();
    setLoginError('');
    setLoginSubmitting(true);
    try {
      const email = loginEmail.trim().toLowerCase();
      const adminSnap = await getDoc(doc(db, 'users', email));

      if (!adminSnap.exists()) {
        setLoginError('No admin user found with this email.');
        return;
      }

      const adminUser = { ...adminSnap.data(), email: adminSnap.id || email };
      if (adminUser.role !== 'Admin') {
        setLoginError('This account is not marked as Admin.');
        return;
      }

      if ((adminUser.password || '') !== loginPassword) {
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

  const handleLogout = () => {
    window.localStorage.removeItem('bazaarAdminSession');
    setAuthUser(null);
    setUseMockData(false);
    setDbError(null);
  };

  const seedFirestoreDatabase = async () => {
    if (useMockData) {
      alert('Please connect to your live Firebase project to seed data.');
      return;
    }
    if (!window.confirm('This will seed default users, products, orders, service areas, and coupons to your Firestore database. Continue?')) return;

    try {
      setLoading(true);
      setDbStatusMsg('Seeding database. Please wait...');
      for (const u of defaultUsers) await setDoc(doc(db, 'users', u.email), u);
      for (const p of defaultProducts) await setDoc(doc(db, 'products', p.id.toString()), p);
      for (const o of defaultOrders) await setDoc(doc(db, 'orders', o.orderId), o);
      for (const a of defaultAreas) await setDoc(doc(db, 'service_areas', a.id), { city: a.city, pinCode: a.pinCode });
      for (const c of defaultCoupons) await setDoc(doc(db, 'coupons', c.code), { code: c.code, amount: c.amount });

      setDbStatusMsg('Database successfully seeded with default catalog data.');
      setTimeout(() => setDbStatusMsg(''), 5000);
    } catch (e) {
      alert(`Error seeding Firestore: ${e.message}`);
    } finally {
      setLoading(false);
    }
  };

  const exportDatabaseToJson = () => {
    const dataExport = {
      users,
      products,
      orders,
      service_areas: areas,
      coupons: coupons,
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

  const importDatabaseFromJson = event => {
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
        if (!window.confirm(`Found ${importedData.users.length} users, ${importedData.products.length} products, ${importedData.orders.length} orders, ${importedData.service_areas?.length || 0} service areas, and ${importedData.coupons?.length || 0} coupons. Import them into your database?`)) return;

        setLoading(true);
        setDbStatusMsg('Importing data. Please wait...');
        for (const u of importedData.users) if (u.email) await setDoc(doc(db, 'users', u.email), u);
        for (const p of importedData.products) if (p.id) await setDoc(doc(db, 'products', p.id.toString()), p);
        for (const o of importedData.orders) if (o.orderId) await setDoc(doc(db, 'orders', o.orderId), o);
        
        if (importedData.service_areas) {
          for (const a of importedData.service_areas) {
            const id = a.id || `area_${Date.now()}_${Math.random()}`;
            await setDoc(doc(db, 'service_areas', id), { city: a.city, pinCode: a.pinCode });
          }
        }
        if (importedData.coupons) {
          for (const c of importedData.coupons) {
            if (c.code) await setDoc(doc(db, 'coupons', c.code), { code: c.code, amount: c.amount });
          }
        }

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

  const patchUser = async (userOrEmail, payload) => {
    let docId = '';
    let email = '';
    if (userOrEmail && typeof userOrEmail === 'object') {
      docId = userOrEmail.docId || userOrEmail.email;
      email = userOrEmail.email;
    } else {
      const found = users.find(u => u.email === userOrEmail);
      docId = found?.docId || userOrEmail;
      email = userOrEmail;
    }

    if (useMockData) {
      setUsers(users.map(u => (u.email === email ? { ...u, ...payload } : u)));
      return;
    }
    try {
      await updateDoc(doc(db, 'users', docId), payload);
    } catch (e) {
      alert(`Error updating user: ${e.message}`);
    }
  };

  // Explicit verify and reject actions
  const handleVerifySeller = (user, verify) => {
    patchUser(user, {
      isSellerVerified: verify,
      isSellerVerificationPending: false,
      isSellerRejected: !verify
    });
  };

  const handleVerifyDeliveryPartner = (user, verify) => {
    patchUser(user, {
      isDeliveryPartnerVerified: verify,
      isDeliveryPartnerVerificationPending: false,
      isDeliveryPartnerRejected: !verify
    });
  };

  // Edit requests approval and rejection
  const handleApproveEditRequest = async user => {
    const updatedFields = {
      editRequestPending: false,
    };
    
    const newName = getRequestedValue(user, 'name', null);
    if (newName && newName !== user.name) updatedFields.name = newName;
    
    if (user.role === 'Seller') {
      const newShopName = getRequestedValue(user, 'shopName', null);
      const newShopAddress = getRequestedValue(user, 'shopAddress', null);
      const newSellerMobile = getRequestedValue(user, 'sellerMobile', null);
      
      if (newShopName && newShopName !== user.shopName) updatedFields.shopName = newShopName;
      if (newShopAddress && newShopAddress !== user.shopAddress) updatedFields.shopAddress = newShopAddress;
      if (newSellerMobile && newSellerMobile !== user.sellerMobile) updatedFields.sellerMobile = newSellerMobile;
    } else if (user.role === 'DeliveryPartner') {
      const newDeliveryMobile = getRequestedValue(user, 'deliveryMobile', null);
      const newVehicleType = getRequestedValue(user, 'deliveryVehicleType', null);
      const newVehicleNumber = getRequestedValue(user, 'deliveryVehicleNumber', null);
      
      if (newDeliveryMobile && newDeliveryMobile !== user.deliveryMobile) updatedFields.deliveryMobile = newDeliveryMobile;
      if (newVehicleType && newVehicleType !== user.deliveryVehicleType) updatedFields.deliveryVehicleType = newVehicleType;
      if (newVehicleNumber && newVehicleNumber !== user.deliveryVehicleNumber) updatedFields.deliveryVehicleNumber = newVehicleNumber;
    }

    updatedFields.requestedName = null;
    updatedFields.requestedShopName = null;
    updatedFields.requestedShopAddress = null;
    updatedFields.requestedSellerMobile = null;
    updatedFields.requestedDeliveryMobile = null;
    updatedFields.requestedDeliveryVehicleType = null;
    updatedFields.requestedDeliveryVehicleNumber = null;
    updatedFields.editRequest = null;

    await patchUser(user, updatedFields);
    alert('Profile edit request successfully approved and updated.');
  };

  const handleRejectEditRequest = async user => {
    if (!window.confirm('Are you sure you want to reject this profile edit request?')) return;
    await patchUser(user, {
      editRequestPending: false,
      requestedName: null,
      requestedShopName: null,
      requestedShopAddress: null,
      requestedSellerMobile: null,
      requestedDeliveryMobile: null,
      requestedDeliveryVehicleType: null,
      requestedDeliveryVehicleNumber: null,
      editRequest: null
    });
    alert('Profile edit request rejected.');
  };

  const deleteUser = async email => {
    if (!window.confirm(`Are you sure you want to delete user ${email}?`)) return;
    const found = users.find(u => u.email === email);
    const docId = found?.docId || email;
    if (useMockData) {
      setUsers(users.filter(u => u.email !== email));
      return;
    }
    try {
      await deleteDoc(doc(db, 'users', docId));
    } catch (e) {
      alert(`Error deleting user: ${e.message}`);
    }
  };

  // Service Areas CRUD
  const handleAddArea = async e => {
    e.preventDefault();
    if (!newCityName || !newPinCode) {
      alert('Please fill in both City Name and Pin Code.');
      return;
    }
    const areaData = { city: newCityName.trim(), pinCode: newPinCode.trim() };
    if (useMockData) {
      const newId = `area_${Date.now()}`;
      setAreas([...areas, { id: newId, ...areaData }]);
      setNewCityName('');
      setNewPinCode('');
      return;
    }
    try {
      const docRef = doc(collection(db, 'service_areas'));
      await setDoc(docRef, areaData);
      setNewCityName('');
      setNewPinCode('');
    } catch (e) {
      alert(`Error adding area: ${e.message}`);
    }
  };

  const handleDeleteArea = async id => {
    if (!window.confirm('Are you sure you want to delete this service area?')) return;
    if (useMockData) {
      setAreas(areas.filter(a => a.id !== id));
      return;
    }
    try {
      await deleteDoc(doc(db, 'service_areas', id));
    } catch (e) {
      alert(`Error deleting area: ${e.message}`);
    }
  };

  // Coupons CRUD
  const handleAddCoupon = async e => {
    e.preventDefault();
    const amountVal = parseFloat(newCouponAmount);
    if (!newCouponCode || Number.isNaN(amountVal) || amountVal <= 0) {
      alert('Please enter a valid coupon code and offered amount.');
      return;
    }
    const couponData = { code: newCouponCode.trim().toUpperCase(), amount: amountVal };
    if (useMockData) {
      const newId = `coupon_${Date.now()}`;
      setCoupons([...coupons, { id: newId, ...couponData }]);
      setNewCouponCode('');
      setNewCouponAmount('');
      return;
    }
    try {
      await setDoc(doc(db, 'coupons', couponData.code), couponData);
      setNewCouponCode('');
      setNewCouponAmount('');
    } catch (e) {
      alert(`Error adding coupon: ${e.message}`);
    }
  };

  const handleDeleteCoupon = async id => {
    if (!window.confirm('Are you sure you want to delete this coupon?')) return;
    if (useMockData) {
      setCoupons(coupons.filter(c => c.id !== id));
      return;
    }
    try {
      await deleteDoc(doc(db, 'coupons', id));
    } catch (e) {
      alert(`Error deleting coupon: ${e.message}`);
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
            ['service_areas', MapPin, 'Service Areas'],
            ['coupons', Tag, 'Coupons']
          ].map(([tab, Icon, label]) => (
            <button key={tab} className={`nav-item ${activeTab === tab ? 'active' : ''}`} onClick={() => setActiveTab(tab)}>
              <Icon size={20} />
              {label}
              {tab === 'users' && pendingSellersCount + pendingPartnersCount + pendingEditRequestsCount > 0 && <span className="nav-count">{pendingSellersCount + pendingPartnersCount + pendingEditRequestsCount}</span>}
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
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button className="btn btn-secondary btn-sm" onClick={() => { setUseMockData(true); setDbError(null); }}>Use Mock Data</button>
              <button className="btn btn-secondary btn-sm" onClick={() => window.location.reload()}>Retry</button>
            </div>
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
              {activeTab === 'service_areas' && 'Service Areas Availability'}
              {activeTab === 'coupons' && 'Coupon Codes Management'}
            </h1>
            <p>
              {activeTab === 'dashboard' && 'Monitor marketplace stats, verification queues, revenue, and data tools.'}
              {activeTab === 'users' && 'Review buyers, sellers, delivery partners, admin accounts, and verification documents.'}
              {activeTab === 'products' && 'Add new items, manage featured products, and review seller inventory.'}
              {activeTab === 'orders' && 'Update workflow statuses, delivery assignment, payment details, and order flags.'}
              {activeTab === 'service_areas' && 'Add and manage pin codes and city names to control checkout availability.'}
              {activeTab === 'coupons' && 'Define coupon offer discount values available for user checkout discount.'}
            </p>
          </div>
        </header>

        {loading ? (
          <div className="empty-state">
            <RefreshCw className="empty-state-icon" style={{ animation: 'spin 1.5s linear infinite' }} size={48} />
            <h3>Syncing with Firebase...</h3>
          </div>
        ) : (
          <>
            {activeTab === 'dashboard' && (
              <div>
                {/* 6 Premium Metrics Cards */}
                <div className="metrics-grid">
                  <Metric title="Total Revenue" value={formatCurrency(totalRevenue)} icon={CheckCircle} />
                  <Metric title="Total Products" value={products.length} icon={ShoppingBag} />
                  <Metric title="Total Users" value={buyerCount} icon={UsersIcon} />
                  <Metric title="Total Sellers" value={`${activeSellersCount}/${sellerCount}`} icon={ShieldCheck} />
                  <Metric title="Total Delivery Partners" value={partnerCount} icon={Clock} />
                  <Metric title="Total Orders" value={orders.length} icon={FileText} />
                </div>

                <div className="mini-metrics-grid">
                  <Metric title="Pending Sellers" value={pendingSellersCount} icon={AlertTriangle} compact />
                  <Metric title="Pending Partners" value={pendingPartnersCount} icon={AlertTriangle} compact />
                  <Metric title="Service Areas" value={areas.length} icon={MapPin} compact />
                  <Metric title="Offered Coupons" value={coupons.length} icon={Tag} compact />
                </div>

                {/* Database Backup & Seed Tools */}
                <div className="glass-panel section-card data-tools">
                  <div className="section-header">
                    <h2><Sparkles size={20} /> Firestore Database Control Center</h2>
                  </div>
                  <p>Seed Firestore database with default sample data catalog, export live data to JSON file, or restore tables from a local JSON backup.</p>
                  <div className="toolbar-row">
                    <button className="btn btn-primary" onClick={seedFirestoreDatabase} disabled={useMockData}>
                      <Sparkles size={16} /> Seed Default Catalog
                    </button>
                    <button className="btn btn-secondary" onClick={exportDatabaseToJson}>
                      <FileText size={16} /> Export JSON Data
                    </button>
                    <button className="btn btn-secondary" onClick={() => fileInputRef.current.click()} disabled={useMockData}>
                      <RefreshCw size={16} /> Import JSON Backup
                    </button>
                    <input type="file" ref={fileInputRef} onChange={importDatabaseFromJson} accept=".json" hidden />
                  </div>
                </div>

                <div className="dashboard-grid">
                  <RecentOrders orders={orders} setActiveTab={setActiveTab} formatCurrency={formatCurrency} statusClass={statusClass} />
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
                      <input value={userSearch} onChange={e => setUserSearch(e.target.value)} placeholder="Search accounts..." />
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
                          <th>Manage Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredUsers.map(user => (
                          <React.Fragment key={user.email}>
                            <tr style={(user.isDisabled || user.disabled) ? { opacity: 0.6 } : {}}>
                              <td>
                                <div className="identity-cell">
                                  <div className="avatar">{(user.name || user.email || 'U').substring(0, 2).toUpperCase()}</div>
                                  <div>
                                    <p>{user.name || 'N/A'}</p>
                                    <span>{user.email}</span>
                                    {hasEditRequest(user) && (
                                      <div className="badge-edit-request">
                                        Edit Request
                                      </div>
                                    )}
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
                                  {/* Verify / Reject explicit buttons for Sellers */}
                                  {user.role === 'Seller' && (
                                    <>
                                      {!user.isSellerVerified && (
                                        <button className="btn-compact btn-verify" onClick={() => handleVerifySeller(user, true)}>
                                          <CheckCircle size={13} /> Verify
                                        </button>
                                      )}
                                      {(user.isSellerVerified || !user.isSellerRejected) && (
                                        <button className="btn-compact btn-reject" onClick={() => handleVerifySeller(user, false)}>
                                          <UserX size={13} /> Reject
                                        </button>
                                      )}
                                    </>
                                  )}

                                  {/* Verify / Reject explicit buttons for Delivery Partners */}
                                  {user.role === 'DeliveryPartner' && (
                                    <>
                                      {!user.isDeliveryPartnerVerified && (
                                        <button className="btn-compact btn-verify" onClick={() => handleVerifyDeliveryPartner(user, true)}>
                                          <CheckCircle size={13} /> Verify
                                        </button>
                                      )}
                                      {(user.isDeliveryPartnerVerified || !user.isDeliveryPartnerRejected) && (
                                        <button className="btn-compact btn-reject" onClick={() => handleVerifyDeliveryPartner(user, false)}>
                                          <UserX size={13} /> Reject
                                        </button>
                                      )}
                                    </>
                                  )}

                                  {/* Disable toggle button for normal users / buyers (or all non-admins) */}
                                  {user.role !== 'Admin' && (
                                    <button 
                                      className={`btn-compact ${(user.isDisabled || user.disabled) ? 'btn-enable' : 'btn-disable'}`}
                                      onClick={() => patchUser(user, { isDisabled: !(user.isDisabled || user.disabled) })}
                                    >
                                      {(user.isDisabled || user.disabled) ? <CheckCircle size={13} /> : <Ban size={13} />}
                                      {(user.isDisabled || user.disabled) ? 'Enable' : 'Disable'}
                                    </button>
                                  )}

                                  <button className="btn-compact btn-details" onClick={() => setExpandedUserEmail(expandedUserEmail === user.email ? '' : user.email)}>Details</button>
                                  <button className="btn-compact btn-delete" onClick={() => deleteUser(user.email)} title="Delete Account"><Trash2 size={13} /></button>
                                </div>
                              </td>
                            </tr>
                            {expandedUserEmail === user.email && (
                              <tr className="detail-row">
                                <td colSpan="5">
                                  <div className="detail-grid">
                                    <DetailLine label="Language Preference" value={user.selectedLanguage} />
                                    <DetailLine label="Plus Club Member" value={user.isPlusMember ? 'Yes' : 'No'} />
                                    <DetailLine label="App Notifications" value={user.notificationsEnabled ? 'Enabled' : 'Disabled'} />
                                    
                                    {user.role === 'Seller' && (
                                      <>
                                        <DetailLine label="Shop Name" value={user.shopName} />
                                        <DetailLine label="Shop Address" value={user.shopAddress} />
                                        <DetailLine label="Aadhaar ID Card" value={user.sellerAadhaar} />
                                        <DetailLine label="PAN Card Number" value={user.sellerPanCard} />
                                        <DetailLine label="GST registration" value={user.sellerGstNumber} />
                                      </>
                                    )}

                                    {user.role === 'DeliveryPartner' && (
                                      <>
                                        <DetailLine label="Vehicle Classification" value={user.deliveryVehicleType} />
                                        <DetailLine label="Vehicle Reg Number" value={user.deliveryVehicleNumber} />
                                        <DetailLine label="Aadhaar ID Card" value={user.deliveryAadhaar} />
                                        <DetailLine label="Emergency Contact No" value={user.deliveryEmergencyContact} />
                                      </>
                                    )}

                                    {(!user.role || user.role === 'User' || user.role === 'Admin') && (
                                      <>
                                        <DetailLine label="Delivery Address" value={user.savedAddress} />
                                        <DetailLine label="Saved Credit/Debit Cards" value={user.savedCards} />
                                      </>
                                    )}

                                    {/* Bank Details section shown permanently for sellers & delivery partners */}
                                    {(user.role === 'Seller' || user.role === 'DeliveryPartner') && (
                                      <div className="bank-details-card">
                                        <h4>
                                          <CheckCircle size={16} /> Bank Account Settlement Details
                                        </h4>
                                        <div className="bank-details-grid">
                                          <p><strong>Holder Name:</strong> {user.bankHolderName || user.name || 'N/A'}</p>
                                          <p><strong>Bank Name:</strong> {user.bankName || 'N/A'}</p>
                                          <p><strong>Account Number:</strong> {user.bankAccountNumber || user.sellerBankAccount || user.deliveryBankAccount || 'N/A'}</p>
                                          <p><strong>IFSC Code:</strong> {user.bankIfscCode || user.sellerIfsc || user.deliveryIfsc || 'N/A'}</p>
                                        </div>
                                      </div>
                                    )}

                                    {/* Profile Edit requests review layout with Approve / Reject flow */}
                                    {hasEditRequest(user) && (
                                      <div className="edit-request-box">
                                        <h4>
                                          <AlertTriangle size={18} /> Profile Modification Request
                                        </h4>
                                        <div className="edit-request-comparison">
                                          <div>
                                            <p style={{ fontWeight: 700, marginBottom: '0.4rem', fontSize: '0.85rem', textTransform: 'uppercase', color: 'var(--text-secondary)' }}>Current Profile Details</p>
                                            <div className="edit-request-panel">
                                              <p><strong>Name:</strong> {user.name || 'N/A'}</p>
                                              {user.role === 'Seller' && (
                                                <>
                                                  <p><strong>Shop Name:</strong> {user.shopName || 'N/A'}</p>
                                                  <p><strong>Shop Address:</strong> {user.shopAddress || 'N/A'}</p>
                                                  <p><strong>Mobile No:</strong> {user.sellerMobile || 'N/A'}</p>
                                                </>
                                              )}
                                              {user.role === 'DeliveryPartner' && (
                                                <>
                                                  <p><strong>Mobile No:</strong> {user.deliveryMobile || 'N/A'}</p>
                                                  <p><strong>Vehicle Classification:</strong> {user.deliveryVehicleType || 'N/A'}</p>
                                                  <p><strong>Vehicle Number:</strong> {user.deliveryVehicleNumber || 'N/A'}</p>
                                                </>
                                              )}
                                            </div>
                                          </div>
                                          <div>
                                            <p style={{ fontWeight: 700, marginBottom: '0.4rem', fontSize: '0.85rem', textTransform: 'uppercase', color: 'var(--color-warning)' }}>Requested Modifications</p>
                                            <div className="edit-request-panel requested">
                                              <p><strong>Name:</strong> <span className={getRequestedValue(user, 'name', user.name) !== user.name ? 'edit-request-diff' : ''}>{getRequestedValue(user, 'name', user.name)}</span></p>
                                              {user.role === 'Seller' && (
                                                <>
                                                  <p><strong>Shop Name:</strong> <span className={getRequestedValue(user, 'shopName', user.shopName) !== user.shopName ? 'edit-request-diff' : ''}>{getRequestedValue(user, 'shopName', user.shopName)}</span></p>
                                                  <p><strong>Shop Address:</strong> <span className={getRequestedValue(user, 'shopAddress', user.shopAddress) !== user.shopAddress ? 'edit-request-diff' : ''}>{getRequestedValue(user, 'shopAddress', user.shopAddress)}</span></p>
                                                  <p><strong>Mobile No:</strong> <span className={getRequestedValue(user, 'sellerMobile', user.sellerMobile) !== user.sellerMobile ? 'edit-request-diff' : ''}>{getRequestedValue(user, 'sellerMobile', user.sellerMobile)}</span></p>
                                                </>
                                              )}
                                              {user.role === 'DeliveryPartner' && (
                                                <>
                                                  <p><strong>Mobile No:</strong> <span className={getRequestedValue(user, 'deliveryMobile', user.deliveryMobile) !== user.deliveryMobile ? 'edit-request-diff' : ''}>{getRequestedValue(user, 'deliveryMobile', user.deliveryMobile)}</span></p>
                                                  <p><strong>Vehicle Classification:</strong> <span className={getRequestedValue(user, 'deliveryVehicleType', user.deliveryVehicleType) !== user.deliveryVehicleType ? 'edit-request-diff' : ''}>{getRequestedValue(user, 'deliveryVehicleType', user.deliveryVehicleType)}</span></p>
                                                  <p><strong>Vehicle Number:</strong> <span className={getRequestedValue(user, 'deliveryVehicleNumber', user.deliveryVehicleNumber) !== user.deliveryVehicleNumber ? 'edit-request-diff' : ''}>{getRequestedValue(user, 'deliveryVehicleNumber', user.deliveryVehicleNumber)}</span></p>
                                                </>
                                              )}
                                            </div>
                                          </div>
                                        </div>
                                        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                                          <button className="btn btn-sm btn-primary" style={{ backgroundColor: 'var(--color-primary)' }} onClick={() => handleApproveEditRequest(user)}>Approve Changes</button>
                                          <button className="btn btn-sm btn-danger" onClick={() => handleRejectEditRequest(user)}>Reject Changes</button>
                                        </div>
                                      </div>
                                    )}
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
                      <input value={orderSearch} onChange={e => setOrderSearch(e.target.value)} placeholder="Search orders..." />
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
                          <th>Order ID</th>
                          <th>Customer</th>
                          <th>Product Name / Items</th>
                          <th>Seller Name</th>
                          <th>Delivery Partner</th>
                          <th>Amount & Method</th>
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
                                <p>{getOrderCustomerName(order)}</p>
                                <span className="table-subtext">{order.email}</span>
                              </td>
                              <td>
                                <p className="clamp" title={order.itemsSummary}>{order.itemsSummary}</p>
                                {order.couponApplied && <span className="status-badge verified" style={{ fontSize: '0.65rem', marginTop: '0.2rem', display: 'inline-flex' }}>Coupon: {order.couponApplied}</span>}
                              </td>
                              <td>
                                <p>{getOrderSeller(order)}</p>
                              </td>
                              <td>
                                <p style={{ color: order.deliveryPartnerEmail ? 'var(--color-accent)' : 'var(--text-secondary)', fontWeight: order.deliveryPartnerEmail ? '700' : 'normal' }}>
                                  {getOrderDeliveryPartner(order)}
                                </p>
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
                                  <select className="form-control compact-select" style={{ minWidth: '130px' }} value={order.status || 'Pending'} onChange={e => updateOrder(order.orderId, { status: e.target.value })}>
                                    {ORDER_STATUSES.map(status => <option key={status} value={status}>{status}</option>)}
                                  </select>
                                  <button className="btn btn-secondary btn-sm" onClick={() => setExpandedOrderId(expandedOrderId === order.orderId ? '' : order.orderId)}>Details</button>
                                </div>
                              </td>
                            </tr>
                            {expandedOrderId === order.orderId && (
                              <tr className="detail-row">
                                <td colSpan="8">
                                  <div className="detail-grid">
                                    <DetailLine label="Customer Profile Name" value={getOrderCustomerName(order)} />
                                    <DetailLine label="Customer Account Email" value={order.email} />
                                    <DetailLine label="Products items" value={order.itemsSummary} />
                                    <DetailLine label="Assigned Seller Shop" value={getOrderSeller(order)} />
                                    <DetailLine label="Accepting Delivery Rider" value={getOrderDeliveryPartner(order)} />
                                    <DetailLine label="Delivery Dispatch Status" value={order.deliveryStatus || 'Not started'} />
                                    <DetailLine label="Seller Confirmed Checkout" value={order.sellerConfirmed ? 'Yes' : 'No'} />
                                    <DetailLine label="Seller Rejected Order" value={order.sellerRejectRequested ? 'Yes' : 'No'} />
                                    <DetailLine label="Delivery Assignment Request" value={order.sellerChangeDeliveryBoyRequested ? 'Yes' : 'No'} />
                                    <DetailLine label="Full Dispatch Destination" value={order.deliveryAddress} />
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

            {/* Service Areas Tab View (Feature 5) */}
            {activeTab === 'service_areas' && (
              <div className="glass-panel section-card">
                <div className="section-header stacked-section-header">
                  <h2>Service Areas Availability</h2>
                </div>
                
                <form onSubmit={handleAddArea} className="form-grid" style={{ marginBottom: '2.5rem', padding: '1.25rem', border: '1px solid var(--border-glass)', borderRadius: 'var(--radius-md)', background: 'rgba(255,255,255,0.01)' }}>
                  <h3 style={{ gridColumn: 'span 2', fontSize: '1.1rem', color: 'var(--color-accent)', marginBottom: '0.5rem' }}>Add New Serviced Area</h3>
                  <div className="form-group">
                    <label className="form-label">City Name</label>
                    <input type="text" className="form-control" value={newCityName} onChange={e => setNewCityName(e.target.value)} placeholder="e.g. Eco City" required />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Pin Code</label>
                    <input type="text" className="form-control" value={newPinCode} onChange={e => setNewPinCode(e.target.value)} placeholder="e.g. 54002" required />
                  </div>
                  <div className="form-actions" style={{ gridColumn: 'span 2', marginTop: '0.5rem', paddingTop: '0.75rem' }}>
                    <button type="submit" className="btn btn-primary">
                      <Plus size={16} /> Add Area
                    </button>
                  </div>
                </form>

                <div className="section-header stacked-section-header">
                  <h3>Active Service Areas</h3>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                    Only added pincodes will show as available for delivery checks. All other locations will show as "Delivery Not Available".
                  </p>
                </div>

                {areas.length === 0 ? (
                  <Empty icon={MapPin} title="No Service Areas Added" text="Add active delivery pin codes above to enable checkout eligibility checks." />
                ) : (
                  <div className="table-container">
                    <table className="modern-table">
                      <thead>
                        <tr>
                          <th>City Name</th>
                          <th>Pin Code</th>
                          <th>Coverage Status</th>
                          <th>Manage</th>
                        </tr>
                      </thead>
                      <tbody>
                        {areas.map(area => (
                          <tr key={area.id}>
                            <td><strong>{area.city}</strong></td>
                            <td className="accent-text" style={{ fontSize: '1.05rem', fontWeight: '800' }}>{area.pinCode}</td>
                            <td>
                              <span className="status-badge verified">Serviced</span>
                            </td>
                            <td>
                              <button className="btn btn-danger btn-sm" onClick={() => handleDeleteArea(area.id)}>
                                <Trash2 size={14} /> Remove Area
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {/* Coupons Tab View (Feature 8) */}
            {activeTab === 'coupons' && (
              <div className="glass-panel section-card">
                <div className="section-header stacked-section-header">
                  <h2>Discount Coupons Offered</h2>
                </div>

                <form onSubmit={handleAddCoupon} className="form-grid" style={{ marginBottom: '2.5rem', padding: '1.25rem', border: '1px solid var(--border-glass)', borderRadius: 'var(--radius-md)', background: 'rgba(255,255,255,0.01)' }}>
                  <h3 style={{ gridColumn: 'span 2', fontSize: '1.1rem', color: 'var(--color-accent)', marginBottom: '0.5rem' }}>Create Offered Coupon</h3>
                  <div className="form-group">
                    <label className="form-label">Coupon Code (Uppercase)</label>
                    <input type="text" className="form-control" value={newCouponCode} onChange={e => setNewCouponCode(e.target.value)} placeholder="e.g. BAZAAR50" required />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Discount Amount Offered (₹)</label>
                    <input type="number" className="form-control" value={newCouponAmount} onChange={e => setNewCouponAmount(e.target.value)} placeholder="e.g. 50" required />
                  </div>
                  <div className="form-actions" style={{ gridColumn: 'span 2', marginTop: '0.5rem', paddingTop: '0.75rem' }}>
                    <button type="submit" className="btn btn-primary">
                      <Plus size={16} /> Create Coupon
                    </button>
                  </div>
                </form>

                <div className="section-header stacked-section-header">
                  <h3>Active Coupon Codes</h3>
                </div>

                {coupons.length === 0 ? (
                  <Empty icon={Tag} title="No Active Coupons" text="Create coupon discount codes above to allow user savings at checkout." />
                ) : (
                  <div className="table-container">
                    <table className="modern-table">
                      <thead>
                        <tr>
                          <th>Coupon Code</th>
                          <th>Offered Amount</th>
                          <th>Status</th>
                          <th>Manage</th>
                        </tr>
                      </thead>
                      <tbody>
                        {coupons.map(coupon => (
                          <tr key={coupon.id || coupon.code}>
                            <td className="accent-text" style={{ fontSize: '1.1rem', fontWeight: '900' }}>{coupon.code}</td>
                            <td><strong>{formatCurrency(coupon.amount)} Off</strong></td>
                            <td>
                              <span className="status-badge verified">Active</span>
                            </td>
                            <td>
                              <button className="btn btn-danger btn-sm" onClick={() => handleDeleteCoupon(coupon.id || coupon.code)}>
                                <Trash2 size={14} /> Delete Coupon
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </main>

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
                <input type="text" className="form-control" value={newProductCat} onChange={e => setNewProductCat(e.target.value)} />
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
    </div>
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

function RecentOrders({ orders, setActiveTab, formatCurrency, statusClass }) {
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
  if (user.isDisabled || user.disabled) {
    return <span className="status-badge cancelled">Disabled</span>;
  }
  if (user.role === 'Seller') {
    if (user.isSellerVerified) {
      return <span className="status-badge verified">Verified Seller</span>;
    }
    if (user.isSellerRejected) {
      return <span className="status-badge cancelled">Rejected Seller</span>;
    }
    return <span className="status-badge pending">Seller Review</span>;
  }
  if (user.role === 'DeliveryPartner') {
    if (user.isDeliveryPartnerVerified) {
      return <span className="status-badge verified">Verified Partner</span>;
    }
    if (user.isDeliveryPartnerRejected) {
      return <span className="status-badge cancelled">Rejected Partner</span>;
    }
    return <span className="status-badge pending">Partner Review</span>;
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
                  <td><button className="btn-compact btn-delete" onClick={() => deleteProduct(product.id)} title="Delete Product"><Trash2 size={14} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default App;
