import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { ActivityIndicator, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../context/AuthContext';
import { C } from '../theme';

import LoginScreen            from '../screens/LoginScreen';
import HomeScreen             from '../screens/HomeScreen';
import ProductListScreen      from '../screens/inventory/ProductListScreen';
import ProductDetailScreen    from '../screens/inventory/ProductDetailScreen';
import SaleListScreen         from '../screens/sales/SaleListScreen';
import NewSaleScreen          from '../screens/sales/NewSaleScreen';
import VoucherScreen          from '../screens/sales/VoucherScreen';
import SaleDetailScreen       from '../screens/sales/SaleDetailScreen';
import NewSaleReturnScreen    from '../screens/sales/NewSaleReturnScreen';
import BookingListScreen      from '../screens/bookings/BookingListScreen';
import BookingDetailScreen    from '../screens/bookings/BookingDetailScreen';
import NewBookingScreen       from '../screens/bookings/NewBookingScreen';
import DoneServiceJobsScreen  from '../screens/bookings/DoneServiceJobsScreen';
import ServiceJobListScreen   from '../screens/servicejobs/ServiceJobListScreen';
import ServiceJobDetailScreen from '../screens/servicejobs/ServiceJobDetailScreen';
import NewServiceJobScreen    from '../screens/servicejobs/NewServiceJobScreen';
import AboutScreen            from '../screens/AboutScreen';
import AccountSettingsScreen  from '../screens/AccountSettingsScreen';
import ChatScreen             from '../screens/ChatScreen';
import StaffReportScreen      from '../screens/StaffReportScreen';
import AuditLogScreen         from '../screens/AuditLogScreen';
import SalesRankingScreen     from '../screens/SalesRankingScreen';
import ExpenseScreen          from '../screens/ExpenseScreen';

const Stack = createNativeStackNavigator();
const Tab   = createBottomTabNavigator();

const HDR = {
  headerStyle: { backgroundColor: C.primary },
  headerTintColor: '#fff' as const,
  headerTitleStyle: { fontWeight: '700' as const },
};

function InventoryStack() {
  return (
    <Stack.Navigator screenOptions={HDR}>
      <Stack.Screen name="Products"      component={ProductListScreen}   options={{ title: 'Products' }} />
      <Stack.Screen name="ProductDetail" component={ProductDetailScreen} options={{ title: 'Product Detail' }} />
    </Stack.Navigator>
  );
}

function SalesStack() {
  return (
    <Stack.Navigator screenOptions={HDR}>
      <Stack.Screen name="SaleList"      component={SaleListScreen}      options={{ title: 'Sales' }} />
      <Stack.Screen name="NewSale"       component={NewSaleScreen}       options={{ title: 'New Sale' }} />
      <Stack.Screen name="Voucher"       component={VoucherScreen}       options={{ title: 'Voucher', headerBackVisible: false }} />
      <Stack.Screen name="SaleDetail"    component={SaleDetailScreen}    options={{ title: 'Sale Detail' }} />
      <Stack.Screen name="NewSaleReturn" component={NewSaleReturnScreen} options={{ title: 'Sale Return' }} />
    </Stack.Navigator>
  );
}

function BookingsStack() {
  return (
    <Stack.Navigator screenOptions={HDR}>
      <Stack.Screen name="BookingList"                 component={BookingListScreen}      options={{ title: 'Bookings' }} />
      <Stack.Screen name="BookingDetail"               component={BookingDetailScreen}    options={{ title: 'Booking Detail' }} />
      <Stack.Screen name="NewBooking"                  component={NewBookingScreen}       options={{ title: 'New Booking' }} />
      <Stack.Screen name="DoneServiceJobs"             component={DoneServiceJobsScreen}  options={{ title: 'Done Service Jobs' }} />
      <Stack.Screen name="ServiceJobDetailFromBooking" component={ServiceJobDetailScreen} options={{ title: 'Job Detail' }} />
    </Stack.Navigator>
  );
}

function ServiceStack() {
  return (
    <Stack.Navigator screenOptions={HDR}>
      <Stack.Screen name="ServiceJobList"   component={ServiceJobListScreen}   options={{ title: 'Service Jobs' }} />
      <Stack.Screen name="ServiceJobDetail" component={ServiceJobDetailScreen} options={{ title: 'Job Detail' }} />
      <Stack.Screen name="NewServiceJob"    component={NewServiceJobScreen}    options={{ title: 'New Service Job' }} />
    </Stack.Navigator>
  );
}

type IoniconName = React.ComponentProps<typeof Ionicons>['name'];

function MainTabs() {
  const { hasPermission } = useAuth();

  const showInventory = hasPermission('CAN_ACCESS_PRODUCT_READ');
  const showSales     = hasPermission('CAN_ACCESS_SALE_READ');
  const showBookings  = hasPermission('CAN_ACCESS_BOOKING_READ');
  const showService   = hasPermission('CAN_ACCESS_SERVICE_JOB_READ');

  const tabIcon = (focused: boolean, active: IoniconName, inactive: IoniconName, color: string) => (
    <Ionicons name={focused ? active : inactive} size={23} color={color} />
  );

  return (
    <Tab.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: C.primary },
        headerTintColor: '#fff',
        headerTitleStyle: { fontWeight: '700' as const },
        tabBarActiveTintColor: C.primary,
        tabBarInactiveTintColor: '#94A3B8',
        tabBarStyle: {
          backgroundColor: '#fff',
          borderTopWidth: 0,
          elevation: 16,
          shadowColor: '#1E293B',
          shadowOpacity: 0.10,
          shadowRadius: 16,
          shadowOffset: { width: 0, height: -4 },
          height: 64,
          paddingBottom: 10,
          paddingTop: 6,
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '600' as const,
          marginTop: 1,
        },
      }}
    >
      <Tab.Screen
        name="Home"
        component={HomeScreen}
        options={{
          headerShown: false,
          title: 'Home',
          tabBarIcon: ({ focused, color }) => tabIcon(focused, 'home', 'home-outline', color),
        }}
      />

      {showInventory && (
        <Tab.Screen
          name="Inventory"
          component={InventoryStack}
          options={{
            headerShown: false,
            title: 'Inventory',
            tabBarIcon: ({ focused, color }) => tabIcon(focused, 'cube', 'cube-outline', color),
          }}
        />
      )}

      {showSales && (
        <Tab.Screen
          name="Sales"
          component={SalesStack}
          options={{
            headerShown: false,
            title: 'Sales',
            tabBarIcon: ({ focused, color }) => tabIcon(focused, 'receipt', 'receipt-outline', color),
          }}
        />
      )}

      {showBookings && (
        <Tab.Screen
          name="Bookings"
          component={BookingsStack}
          options={{
            headerShown: false,
            title: 'Bookings',
            tabBarIcon: ({ focused, color }) => tabIcon(focused, 'calendar', 'calendar-outline', color),
          }}
        />
      )}

      {showService && (
        <Tab.Screen
          name="Service"
          component={ServiceStack}
          options={{
            headerShown: false,
            title: 'Service',
            tabBarIcon: ({ focused, color }) => tabIcon(focused, 'construct', 'construct-outline', color),
          }}
        />
      )}
    </Tab.Navigator>
  );
}

export default function AppNavigator() {
  const { isReady, isLoggedIn } = useAuth();

  if (!isReady) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: C.primary }}>
        <ActivityIndicator color="#fff" size="large" />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {isLoggedIn ? (
          <>
            <Stack.Screen name="Main" component={MainTabs} />
            <Stack.Screen name="About"           component={AboutScreen}           options={{ headerShown: true, ...HDR, title: 'About' }} />
            <Stack.Screen name="AccountSettings" component={AccountSettingsScreen} options={{ headerShown: true, ...HDR, title: 'Account Settings' }} />
            <Stack.Screen name="Chat"            component={ChatScreen}            options={{ headerShown: true, ...HDR, title: '💬  Team Chat' }} />
            <Stack.Screen name="StaffReport"     component={StaffReportScreen}     options={{ headerShown: true, ...HDR, title: '📊  Staff Report' }} />
            <Stack.Screen name="AuditLog"        component={AuditLogScreen}        options={{ headerShown: true, ...HDR, title: '🛡️  Audit Logs' }} />
            <Stack.Screen name="SalesRanking"    component={SalesRankingScreen}    options={{ headerShown: true, ...HDR, title: '🏆  Sales Ranking' }} />
            <Stack.Screen name="Expense"         component={ExpenseScreen}         options={{ headerShown: true, ...HDR, title: 'Expenses' }} />
          </>
        ) : (
          <Stack.Screen name="Login" component={LoginScreen} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
