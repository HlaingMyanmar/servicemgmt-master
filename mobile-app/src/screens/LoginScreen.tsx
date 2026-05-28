import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, StyleSheet,
  KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator,
} from 'react-native';
import { useAuth } from '../context/AuthContext';
import { C } from '../theme';

export default function LoginScreen() {
  const { login } = useAuth();
  const [server,   setServer]   = useState('https://192.168.20.253:8080');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState('');

  const handleLogin = async () => {
    if (!server.trim() || !username.trim() || !password.trim()) {
      setError('All fields are required');
      return;
    }
    setLoading(true);
    setError('');
    const err = await login(server, username, password);
    if (err) setError(err);
    setLoading(false);
  };

  return (
    <KeyboardAvoidingView style={st.root} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={st.scroll} keyboardShouldPersistTaps="handled">
        {/* Logo area */}
        <View style={st.logoWrap}>
          <View style={st.logo}>
            <Text style={st.logoText}>S</Text>
          </View>
          <Text style={st.appName}>SSPD Manager</Text>
          <Text style={st.subtitle}>Inventory & Sales</Text>
        </View>

        {/* Form card */}
        <View style={st.card}>
          <Text style={st.fieldLabel}>Server URL</Text>
          <TextInput
            style={st.input}
            value={server}
            onChangeText={setServer}
            placeholder="http://192.168.x.x:8080"
            placeholderTextColor={C.textMuted}
            autoCapitalize="none"
            keyboardType="url"
          />

          <Text style={st.fieldLabel}>Username</Text>
          <TextInput
            style={st.input}
            value={username}
            onChangeText={setUsername}
            placeholder="Username"
            placeholderTextColor={C.textMuted}
            autoCapitalize="none"
          />

          <Text style={st.fieldLabel}>Password</Text>
          <TextInput
            style={st.input}
            value={password}
            onChangeText={setPassword}
            placeholder="Password"
            placeholderTextColor={C.textMuted}
            secureTextEntry
          />

          {error ? <Text style={st.error}>{error}</Text> : null}

          <TouchableOpacity
            style={[st.btn, loading && { opacity: 0.7 }]}
            onPress={handleLogin}
            disabled={loading}
          >
            {loading
              ? <ActivityIndicator color="#fff" />
              : <Text style={st.btnText}>Login</Text>
            }
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const st = StyleSheet.create({
  root:       { flex: 1, backgroundColor: C.primary },
  scroll:     { flexGrow: 1, justifyContent: 'center', padding: 24 },
  logoWrap:   { alignItems: 'center', marginBottom: 32 },
  logo:       { width: 72, height: 72, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.2)', justifyContent: 'center', alignItems: 'center', marginBottom: 12 },
  logoText:   { fontSize: 32, fontWeight: '800', color: '#fff' },
  appName:    { fontSize: 24, fontWeight: '800', color: '#fff', marginBottom: 4 },
  subtitle:   { fontSize: 13, color: 'rgba(255,255,255,0.7)', fontWeight: '600' },
  card:       { backgroundColor: '#fff', borderRadius: 20, padding: 24, shadowColor: '#000', shadowOpacity: 0.1, shadowRadius: 16, elevation: 8 },
  fieldLabel: { fontSize: 11, fontWeight: '700', color: C.textMuted, textTransform: 'uppercase', letterSpacing: 0.5, marginBottom: 6, marginTop: 14 },
  input:      { backgroundColor: C.bg, borderWidth: 1, borderColor: C.border, borderRadius: 10, paddingHorizontal: 14, paddingVertical: 11, fontSize: 14, color: C.text },
  error:      { color: C.danger, fontSize: 13, marginTop: 12, textAlign: 'center', fontWeight: '600' },
  btn:        { backgroundColor: C.primary, borderRadius: 10, paddingVertical: 14, alignItems: 'center', marginTop: 20 },
  btnText:    { color: '#fff', fontWeight: '700', fontSize: 15 },
});
