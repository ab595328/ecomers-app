import React, { useEffect, useMemo, useState } from 'react';
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
import { db, auth } from './firebase';
import { signInWithEmailAndPassword, signOut as firebaseSignOut, onAuthStateChanged } from 'firebase/auth';

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
  { email: 'buyer@bazaar.com', name: 'Green Buyer', role: 'User', isPlusMember: true, savedAddress: '22 Market Street, Eco City', phone: '+91 8888811111' },
  { email: 'seller@store.com', name: 'Organic Farms Co.', role: 'Seller', isSellerVerified: true, isSellerVerificationPending: false, shopName: 'Organic Farms', shopAddress: 'Green Valley Farm Road', sellerMobile: '+91 7777711111', sellerGstNumber: 'GST-ZVB-1001' },
  { email: 'pending.seller@store.com', name: 'Fresh Cart Seller', role: 'Seller', isSellerVerified: false, isSellerVerificationPending: true, shopName: 'Fresh Cart', shopAddress: 'Unit 8, Local Bazaar', sellerMobile: '+91 7777722222' },
  { email: 'rider@bazaar.com', name: 'Fast Courier Rider', role: 'DeliveryPartner', isDeliveryPartnerVerified: true, deliveryMobile: '+91 9999911111', deliveryVehicleType: 'Electric Scooter', deliveryVehicleNumber: 'DL-3C-EC-9999', deliveryEmergencyContact: '+91 9999922222' },
  { email: 'pending.rider@bazaar.com', name: 'New Delivery Partner', role: 'DeliveryPartner', isDeliveryPartnerVerified: false, deliveryMobile: '+91 9999933333', deliveryVehicleType: 'Bike', deliveryVehicleNumber: 'DL-4S-NP-2211' }
];


const defaultOrders = [
  { orderId: 'ORD-9872', email: 'buyer@bazaar.com', orderDate: Date.now() - 3600000 * 2, totalAmount: 134.98, status: 'Processing', itemsSummary: '1x ZYL Sound Pro Wireless ANC, 1x Organic Apples', paymentMode: 'Card ending in 4242', deliveryAddress: '22 Market Street, Eco City', couponApplied: 'GREEN10', deliveryPartnerEmail: '', deliveryStatus: '', sellerConfirmed: false },
  { orderId: 'ORD-5431', email: 'buyer@bazaar.com', orderDate: Date.now() - 3600000 * 24, totalAmount: 4.99, status: 'Delivered', itemsSummary: '1x Premium Farms Organic Apples (1kg)', paymentMode: 'Wallet', deliveryAddress: '22 Market Street, Eco City', deliveryPartnerEmail: 'rider@bazaar.com', deliveryStatus: 'Delivered', sellerConfirmed: true }
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

  const [showProductModal, setShowProductModal] = useState(false);
  const [newProductName, setNewProductName] = useState('');
  const [newProductPrice, setNewProductPrice] = useState('');
  const [newProductOrigPrice, setNewProductOrigPrice] = useState('');
  const [newProductCat, setNewProductCat] = useState('');
  const [newProductDesc, setNewProductDesc] = useState('');
  const [newProductSeller, setNewProductSeller] = useState('admin@bazaar.com');
  const [newProductFeatured, setNewProductFeatured] = useState(false);
  const [newProductImageFile, setNewProductImageFile] = useState(null);
  const [newProductImagePreview, setNewProductImagePreview] = useState('');
  const [newProductImageUrl, setNewProductImageUrl] = useState('');
  const [productImageUploading, setProductImageUploading] = useState(false);
  const [productImageUploadProgress, setProductImageUploadProgress] = useState(0);

  // Settings tab state
  const [appConfig, setAppConfig] = useState({});
  const [serviceCitiesText, setServiceCitiesText] = useState('');
  const [servicePincodesText, setServicePincodesText] = useState('');
  const [payoutDelayHours, setPayoutDelayHours] = useState('24');

  const saveServiceConfig = async (e) => {
    e.preventDefault();
    try {
      const cities = serviceCitiesText.split(',').map(c => c.trim()).filter(Boolean);
      const pincodes = servicePincodesText.split(',').map(p => p.trim()).filter(Boolean);
      await updateDoc(doc(db, 'app_config', 'main'), {
        serviceCities: cities,
        servicePincodes: pincodes,
        payoutDelayHours: parseInt(payoutDelayHours, 10) || 24
      });
      alert('Settings saved successfully!');
    } catch (e) {
      try {
        const cities = serviceCitiesText.split(',').map(c => c.trim()).filter(Boolean);
        const pincodes = servicePincodesText.split(',').map(p => p.trim()).filter(Boolean);
        await setDoc(doc(db, 'app_config', 'main'), {
          serviceCities: cities,
          servicePincodes: pincodes,
          payoutDelayHours: parseInt(payoutDelayHours, 10) || 24
        }, { merge: true });
        alert('Settings saved successfully!');
      } catch (err) {
        alert(`Error saving settings: ${err.message}`);
      }
    }
  };

  // Coupon management states
  const [coupons, setCoupons] = useState([]);
  const [showCouponModal, setShowCouponModal] = useState(false);
  const [newCouponCode, setNewCouponCode] = useState('');
  const [newCouponDiscount, setNewCouponDiscount] = useState('');
  const [newCouponMinOrder, setNewCouponMinOrder] = useState('');
  const [newCouponMaxDiscount, setNewCouponMaxDiscount] = useState('');
  const [newCouponDesc, setNewCouponDesc] = useState('');
  const [newCouponActive, setNewCouponActive] = useState(true);
  // Banners tab state
  const [banners, setBanners] = useState([]);
  const [showBannerModal, setShowBannerModal] = useState(false);
  const [newBannerLabel, setNewBannerLabel] = useState('');
  const [newBannerTitle, setNewBannerTitle] = useState('');
  const [newBannerDesc, setNewBannerDesc] = useState('');
  const [newBannerTargetCat, setNewBannerTargetCat] = useState('All');
  const [newBannerGradientStart, setNewBannerGradientStart] = useState('#1B5E20');
  const [newBannerGradientEnd, setNewBannerGradientEnd] = useState('#A5D6A7');
  const [newBannerSortOrder, setNewBannerSortOrder] = useState('0');
  const [newBannerActive, setNewBannerActive] = useState(true);
  // On app load: restore session from localStorage AND re-authenticate Firebase Auth
  // so Firestore rules (request.auth != null) work after page refresh
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      const savedSession = window.localStorage.getItem('bazaarAdminSession');
      if (firebaseUser && savedSession) {
        // Firebase Auth is restored + we have a local session — trust both
        try {
          const sessionUser = JSON.parse(savedSession);
          if (sessionUser?.email && sessionUser?.role === 'Admin') {
            setAuthUser(sessionUser);
          }
        } catch (e) {
          window.localStorage.removeItem('bazaarAdminSession');
        }
      } else if (savedSession && !firebaseUser) {
        // We have a local session but Firebase Auth expired — clear it
        window.localStorage.removeItem('bazaarAdminSession');
      }
      setAuthLoading(false);
    });

    return () => unsubscribe();
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
      setCoupons([]);
      setLoading(false);
      return undefined;
    }

     let unsubscribeUsers = () => { };
    let unsubscribeProducts = () => { };
    let unsubscribeOrders = () => { };
    let unsubscribeConfig = () => { };
    let unsubscribeCoupons = () => { };
    let unsubscribeBanners = () => { };

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

      unsubscribeBanners = onSnapshot(
        collection(db, 'banners'),
        snapshot => {
          const list = snapshot.docs.map(bannerDoc => ({ ...bannerDoc.data(), id: bannerDoc.id }));
          setBanners(list.sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0)));
        },
        err => {
          console.warn('Firestore banners sync failed:', err);
        }
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
      unsubscribeBanners();
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

  const handleLogin = async event => {
    event.preventDefault();
    setLoginError('');
    setLoginSubmitting(true);
    try {
      const email = loginEmail.trim().toLowerCase();

      // Step 1: Sign into Firebase Auth so Firestore rules (request.auth != null) work
      let firebaseAuthSuccess = false;
      try {
        await signInWithEmailAndPassword(auth, email, loginPassword);
        firebaseAuthSuccess = true;
      } catch (signInErr) {
        // Maybe user doesn't exist in Firebase Auth yet — try creating them
        try {
          const { createUserWithEmailAndPassword } = await import('firebase/auth');
          await createUserWithEmailAndPassword(auth, email, loginPassword);
          firebaseAuthSuccess = true;
          console.log('Auto-created Firebase Auth account for admin.');
        } catch (createErr) {
          console.warn('Firebase Auth failed:', createErr.message);
        }
      }

      if (!firebaseAuthSuccess) {
        setLoginError('Firebase Authentication failed. Please add this email manually in Firebase Console → Authentication → Users.');
        return;
      }

      // Step 2: Verify admin role from Firestore (now auth is set, rules will pass)
      const adminSnap = await getDoc(doc(db, 'users', email));

      if (!adminSnap.exists()) {
        setLoginError('No admin user found in Firestore with this email. Please seed the database first.');
        await firebaseSignOut(auth).catch(() => {});
        return;
      }

      const adminUser = { ...adminSnap.data(), email: adminSnap.id || email };
      if (adminUser.role !== 'Admin') {
        setLoginError(`This account is registered as "${adminUser.role}", not Admin.`);
        await firebaseSignOut(auth).catch(() => {});
        return;
      }

      if ((adminUser.password || '') !== loginPassword) {
        setLoginError('Wrong password.');
        await firebaseSignOut(auth).catch(() => {});
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
    firebaseSignOut(auth).catch(() => {});
    setAuthUser(null);
    setUseMockData(false);
    setDbError(null);
  };

  const _seedFirestoreDatabase = async () => {
    if (useMockData) {
      alert('Please connect to your live Firebase project to seed data.');
      return;
    }
    if (!window.confirm('This will seed default users, products, and orders to your Firestore database. Continue?')) return;

    try {
      setLoading(true);
      setDbStatusMsg('Seeding database. Please wait...');
      for (const u of defaultUsers) await setDoc(doc(db, 'users', u.email), u);
      for (const p of defaultProducts) await setDoc(doc(db, 'products', p.id.toString()), p);
      for (const o of defaultOrders) await setDoc(doc(db, 'orders', o.orderId), o);
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
        if (!window.confirm(`Found ${importedData.users.length} users, ${importedData.products.length} products, and ${importedData.orders.length} orders. Import them into your database?`)) return;

        setLoading(true);
        setDbStatusMsg('Importing data. Please wait...');
        for (const u of importedData.users) if (u.email) await setDoc(doc(db, 'users', u.email), u);
        for (const p of importedData.products) if (p.id) await setDoc(doc(db, 'products', p.id.toString()), p);
        for (const o of importedData.orders) if (o.orderId) await setDoc(doc(db, 'orders', o.orderId), o);
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

  // Upload image to Cloudinary using unsigned upload preset
  const uploadToCloudinary = (file) => {
    return new Promise((resolve, reject) => {
      const cloudName = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME;
      const uploadPreset = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET;
      const folder = import.meta.env.VITE_CLOUDINARY_FOLDER || '';

      if (!cloudName || !uploadPreset) {
        reject(new Error('Cloudinary is not configured. Add VITE_CLOUDINARY_CLOUD_NAME and VITE_CLOUDINARY_UPLOAD_PRESET to .env'));
        return;
      }

      const formData = new FormData();
      formData.append('file', file);
      formData.append('upload_preset', uploadPreset);
      formData.append('public_id', `admin_product_${Date.now()}`);
      if (folder) formData.append('folder', folder);

      // Use XMLHttpRequest to track upload progress
      const xhr = new XMLHttpRequest();
      xhr.open('POST', `https://api.cloudinary.com/v1_1/${cloudName}/image/upload`);

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          const progress = Math.round((e.loaded / e.total) * 100);
          setProductImageUploadProgress(progress);
        }
      });

      xhr.onload = () => {
        try {
          const response = JSON.parse(xhr.responseText);
          if (xhr.status >= 200 && xhr.status < 300 && response.secure_url) {
            resolve(response.secure_url);
          } else {
            reject(new Error(response.error?.message || 'Cloudinary upload failed'));
          }
        } catch (e) {
          reject(new Error('Invalid response from Cloudinary'));
        }
      };

      xhr.onerror = () => reject(new Error('Network error during upload'));
      xhr.send(formData);
    });
  };

  const handleProductImageChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setNewProductImageFile(file);
    setNewProductImagePreview(URL.createObjectURL(file));
    setProductImageUploading(true);
    setProductImageUploadProgress(0);
    try {
      const url = await uploadToCloudinary(file);
      setNewProductImageUrl(url);
    } catch (err) {
      alert(`Image upload failed: ${err.message}`);
      setNewProductImageUrl('');
    } finally {
      setProductImageUploading(false);
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
      imageUrlName: newProductImageUrl || '',
      isFeatured: newProductFeatured,
      sellerEmail: newProductSeller || 'admin@bazaar.com',
      extraImages: '',
      stockQuantity: 100
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
    setNewProductImageFile(null);
    setNewProductImagePreview('');
    setNewProductImageUrl('');
    setProductImageUploading(false);
    setProductImageUploadProgress(0);
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

  const toggleBannerActive = async (id, currentStatus) => {
    if (useMockData) {
      setBanners(banners.map(b => (b.id === id ? { ...b, isActive: !currentStatus } : b)));
      return;
    }
    try {
      await updateDoc(doc(db, 'banners', id), { isActive: !currentStatus });
    } catch (e) {
      alert(`Error updating banner: ${e.message}`);
    }
  };

  const deleteBanner = async id => {
    if (!window.confirm(`Are you sure you want to delete this banner?`)) return;
    if (useMockData) {
      setBanners(banners.filter(b => b.id !== id));
      return;
    }
    try {
      await deleteDoc(doc(db, 'banners', id));
    } catch (e) {
      alert(`Error deleting banner: ${e.message}`);
    }
  };

  const handleCreateBanner = async e => {
    e.preventDefault();
    if (!newBannerTitle.trim() || !newBannerLabel.trim()) {
      alert('Please fill in a valid title and label.');
      return;
    }

    const bannerId = `banner_${Date.now()}`;
    const newBann = {
      id: bannerId,
      label: newBannerLabel.trim(),
      title: newBannerTitle.trim(),
      description: newBannerDesc.trim(),
      targetCategory: newBannerTargetCat,
      gradientStart: newBannerGradientStart.trim() || '#1B5E20',
      gradientEnd: newBannerGradientEnd.trim() || '#A5D6A7',
      sortOrder: parseInt(newBannerSortOrder, 10) || 0,
      isActive: newBannerActive
    };

    if (useMockData) {
      setBanners([...banners, newBann]);
      setShowBannerModal(false);
      resetBannerForm();
      return;
    }

    try {
      await setDoc(doc(db, 'banners', bannerId), newBann);
      setShowBannerModal(false);
      resetBannerForm();
    } catch (e) {
      alert(`Error creating banner: ${e.message}`);
    }
  };

  const resetBannerForm = () => {
    setNewBannerLabel('');
    setNewBannerTitle('');
    setNewBannerDesc('');
    setNewBannerTargetCat('All');
    setNewBannerGradientStart('#1B5E20');
    setNewBannerGradientEnd('#A5D6A7');
    setNewBannerSortOrder('0');
    setNewBannerActive(true);
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
            ['coupons', Ticket, 'Coupons'],
            ['banners', Sparkles, 'Banners'],
            ['settings', Settings, 'Serviceable Areas']
          ].map(([tab, Icon, label]) => (
            <button key={tab} className={`nav-item ${activeTab === tab ? 'active' : ''}`} onClick={() => setActiveTab(tab)}>
              <Icon size={20} />
              {label}
              {tab === 'users' && pendingSellersCount + pendingPartnersCount > 0 && <span className="nav-count">{pendingSellersCount + pendingPartnersCount}</span>}
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
              {activeTab === 'coupons' && 'Coupon Directory'}
              {activeTab === 'banners' && 'Marketing Banners'}
              {activeTab === 'settings' && 'Serviceable Area & Payout Settings'}
            </h1>
            <p>
              {activeTab === 'dashboard' && 'Monitor marketplace stats, verification queues, revenue, and data tools.'}
              {activeTab === 'users' && 'Review buyers, sellers, delivery partners, admin accounts, and verification documents.'}
              {activeTab === 'products' && 'Add new items, manage featured products, and review seller inventory.'}
              {activeTab === 'orders' && 'Update workflow statuses, delivery assignment, payment details, and order flags.'}
              {activeTab === 'coupons' && 'Create, configure, and monitor discount promo coupons.'}
              {activeTab === 'banners' && 'Manage rotating carousel billboard advertising banners shown in the mobile app.'}
              {activeTab === 'settings' && 'Control eligible cities, pincodes, and automatic Razorpay payout timing.'}
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

                <div className="glass-panel section-card data-tools">
                  <div className="section-header">
                    <h2><Sparkles size={20} /> Database Tools</h2>
                  </div>
                  <p style={{fontSize:'13px', color:'var(--text-muted)', marginBottom:'12px'}}>
                    Seed Firestore with default sample data for testing, or export/import a JSON backup.
                  </p>
                  <div className="toolbar-row">
                    <button className="btn btn-primary" onClick={_seedFirestoreDatabase}>
                      <Sparkles size={16} /> Seed Default Data
                    </button>
                    <button className="btn btn-secondary" onClick={_exportDatabaseToJson}>
                      Export JSON
                    </button>
                    <label className="btn btn-secondary" style={{cursor:'pointer'}}>
                      Import JSON
                      <input type="file" onChange={_importDatabaseFromJson} accept=".json" hidden />
                    </label>
                  </div>
                </div>

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

            {activeTab === 'coupons' && (
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
            )}

            {activeTab === 'banners' && (
              <div className="glass-panel section-card">
                <div className="section-header stacked-section-header">
                  <h2>Banner Directory</h2>
                  <div className="toolbar-row">
                    <button className="btn btn-primary" onClick={() => setShowBannerModal(true)}>
                      <Plus size={16} /> Create Banner
                    </button>
                  </div>
                </div>

                {banners.length === 0 ? (
                  <Empty icon={Sparkles} title="No Banners Found" text="Create billboard banners to advertise products, category discounts, and special launch events on the mobile app home screen." />
                ) : (
                  <div className="table-container">
                    <table className="modern-table">
                      <thead>
                        <tr>
                          <th>Preview</th>
                          <th>Label</th>
                          <th>Title</th>
                          <th>Description</th>
                          <th>Target Category</th>
                          <th>Sort Order</th>
                          <th>Status</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {banners.map(banner => {
                          const bgGradient = `linear-gradient(135deg, ${banner.gradientStart || '#1B5E20'}, ${banner.gradientEnd || '#A5D6A7'})`;
                          return (
                            <tr key={banner.id}>
                              <td>
                                <div style={{
                                  width: '120px',
                                  height: '60px',
                                  background: bgGradient,
                                  borderRadius: '8px',
                                  display: 'flex',
                                  flexDirection: 'column',
                                  justifyContent: 'center',
                                  padding: '8px',
                                  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                                  color: '#ffffff',
                                  overflow: 'hidden'
                                }}>
                                  <span style={{ fontSize: '7px', background: '#FFD700', color: '#000000', fontWeight: '900', padding: '1px 3px', borderRadius: '3px', alignSelf: 'flex-start' }}>{banner.label}</span>
                                  <span style={{ fontSize: '9px', fontWeight: 'bold', marginTop: '2px', whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>{banner.title}</span>
                                </div>
                              </td>
                              <td>
                                <p className="accent-text" style={{ fontWeight: 'bold' }}>{banner.label}</p>
                              </td>
                              <td>
                                <p style={{ fontWeight: '600' }}>{banner.title}</p>
                              </td>
                              <td className="muted-cell">
                                <p>{banner.description || 'No description'}</p>
                              </td>
                              <td>
                                <span style={{ textTransform: 'capitalize', fontWeight: 'bold', fontSize: '12px' }}>{banner.targetCategory || 'All'}</span>
                              </td>
                              <td>
                                <p>{banner.sortOrder || 0}</p>
                              </td>
                              <td>
                                <span className={`status-badge ${banner.isActive ? 'verified' : 'rejected'}`}>
                                  {banner.isActive ? 'Active' : 'Inactive'}
                                </span>
                              </td>
                              <td>
                                <div className="actions-row">
                                  <button className={`btn btn-sm ${banner.isActive ? 'btn-secondary' : 'btn-primary'}`} onClick={() => toggleBannerActive(banner.id, banner.isActive)}>
                                    {banner.isActive ? 'Deactivate' : 'Activate'}
                                  </button>
                                  <button className="btn btn-danger btn-sm" onClick={() => deleteBanner(banner.id)}>
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
            )}

            {activeTab === 'settings' && (
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
                <label className="form-label">Product Image (Firebase Storage)</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                  <label
                    htmlFor="product-image-upload"
                    className="btn btn-secondary btn-sm"
                    style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                  >
                    <Plus size={14} />
                    {newProductImageFile ? 'Change Image' : 'Upload Image'}
                  </label>
                  <input
                    id="product-image-upload"
                    type="file"
                    accept="image/*"
                    style={{ display: 'none' }}
                    onChange={handleProductImageChange}
                  />
                  {productImageUploading && (
                    <span style={{ fontSize: '12px', color: 'var(--color-accent)' }}>
                      <RefreshCw size={12} style={{ animation: 'spin 1.5s linear infinite', marginRight: '4px' }} />
                      Uploading {productImageUploadProgress}%...
                    </span>
                  )}
                  {newProductImageUrl && !productImageUploading && (
                    <span style={{ fontSize: '11px', color: '#4caf50', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <CheckCircle size={12} /> Image uploaded to Firebase Storage
                    </span>
                  )}
                </div>
                {newProductImagePreview && (
                  <div style={{ marginTop: '10px' }}>
                    <img
                      src={newProductImagePreview}
                      alt="Preview"
                      style={{ width: '120px', height: '120px', objectFit: 'cover', borderRadius: '10px', border: '2px solid var(--color-accent)' }}
                    />
                  </div>
                )}
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
                <button type="submit" className="btn btn-primary" disabled={productImageUploading}>
                  {productImageUploading ? 'Uploading Image...' : 'Publish Item'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showCouponModal && (
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
      )}

      {showBannerModal && (
        <div className="modal-overlay">
          <div className="glass-panel modal-content">
            <button className="close-btn" onClick={() => setShowBannerModal(false)}>x</button>
            <h2 className="modal-title"><Plus size={22} color="var(--color-accent)" /> Create New Banner</h2>
            <form onSubmit={handleCreateBanner} className="form-grid">
              <div className="form-group">
                <label className="form-label">Banner Label (e.g. LAUNCH SPECIAL)</label>
                <input type="text" className="form-control" value={newBannerLabel} onChange={e => setNewBannerLabel(e.target.value)} placeholder="LAUNCH SPECIAL" required />
              </div>
              <div className="form-group">
                <label className="form-label">Banner Title</label>
                <input type="text" className="form-control" value={newBannerTitle} onChange={e => setNewBannerTitle(e.target.value)} placeholder="50% OFF SMART TECH" required />
              </div>
              <div className="form-group full-width">
                <label className="form-label">Banner Description</label>
                <input type="text" className="form-control" value={newBannerDesc} onChange={e => setNewBannerDesc(e.target.value)} placeholder="Futuristic wellness tech with AMOLED screens & rapid charge." />
              </div>
              <div className="form-group">
                <label className="form-label">Target Category Link</label>
                <select className="form-control" value={newBannerTargetCat} onChange={e => setNewBannerTargetCat(e.target.value)}>
                  <option value="All">All Categories</option>
                  <option value="Electronics">Electronics</option>
                  <option value="Fresh Products">Fresh Products</option>
                  <option value="Fashion">Fashion</option>
                  <option value="Home & Kitchen">Home & Kitchen</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Sort Order Index</label>
                <input type="number" className="form-control" value={newBannerSortOrder} onChange={e => setNewBannerSortOrder(e.target.value)} placeholder="0" />
              </div>
              <div className="form-group">
                <label className="form-label">Gradient Start Color (HEX)</label>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input type="color" value={newBannerGradientStart} onChange={e => setNewBannerGradientStart(e.target.value)} style={{ width: '40px', height: '38px', padding: '0', border: '1px solid var(--border-color)', borderRadius: '4px', cursor: 'pointer' }} />
                  <input type="text" className="form-control" value={newBannerGradientStart} onChange={e => setNewBannerGradientStart(e.target.value)} placeholder="#1B5E20" required />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Gradient End Color (HEX)</label>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <input type="color" value={newBannerGradientEnd} onChange={e => setNewBannerGradientEnd(e.target.value)} style={{ width: '40px', height: '38px', padding: '0', border: '1px solid var(--border-color)', borderRadius: '4px', cursor: 'pointer' }} />
                  <input type="text" className="form-control" value={newBannerGradientEnd} onChange={e => setNewBannerGradientEnd(e.target.value)} placeholder="#A5D6A7" required />
                </div>
              </div>
              <div className="form-group full-width checkbox-row">
                <input type="checkbox" id="banner-active-check" checked={newBannerActive} onChange={e => setNewBannerActive(e.target.checked)} />
                <label htmlFor="banner-active-check">Mark as Active instantly</label>
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowBannerModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create Banner</button>
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
                <th>Image</th>
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
                  <td>
                    {product.imageUrlName && (product.imageUrlName.startsWith('http://') || product.imageUrlName.startsWith('https://')) ? (
                      <img
                        src={product.imageUrlName}
                        alt={product.name}
                        style={{
                          width: '52px',
                          height: '52px',
                          objectFit: 'cover',
                          borderRadius: '8px',
                          border: '1px solid #e0e0e0',
                          display: 'block'
                        }}
                        onError={e => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
                      />
                    ) : null}
                    <div style={{
                      width: '52px', height: '52px', borderRadius: '8px',
                      background: 'rgba(0,128,0,0.08)',
                      display: (!product.imageUrlName || (!product.imageUrlName.startsWith('http://') && !product.imageUrlName.startsWith('https://'))) ? 'flex' : 'none',
                      alignItems: 'center', justifyContent: 'center',
                      border: '1px dashed #ccc'
                    }}>
                      <ShoppingBag size={20} style={{ opacity: 0.3 }} />
                    </div>
                  </td>
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

export default App;
