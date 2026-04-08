"use client";

import { useEffect, useState, useRef } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { usersApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import type { GamificationResponse, BadgeType, SpotType, TrustLevel } from "@/types/api";
import { t } from "@/lib/i18n";

const SPOT_TYPES: { value: SpotType; label: string }[] = [
  { value: "STREET", label: "Rua" },
  { value: "PARKING_LOT", label: "Estacionamento" },
  { value: "MALL", label: "Shopping" },
  { value: "TERRAIN", label: "Terreno" },
  { value: "ZONA_AZUL", label: "Zona Azul" },
];
const TRUST_LEVELS: { value: TrustLevel; label: string }[] = [
  { value: "HIGH", label: "Alta" },
  { value: "MEDIUM", label: "Média" },
  { value: "LOW", label: "Baixa" },
  { value: "NO_DATA", label: "Sem dados" },
];

const BADGE_LABELS: Record<BadgeType, string> = {
  FIRST_STEPS: "Primeiros Passos",
  REGULAR: "Frequente",
  VETERAN: "Veterano",
  CENTURION: "Centurião",
  SPOT_DISCOVERER: "Descobridor",
  CARTOGRAPHER: "Cartógrafo",
  RELIABLE: "Confiável",
  NIGHT_OWL: "Coruja",
  EARLY_BIRD: "Madrugador",
  COMMUNITY_GUARDIAN: "Guardião",
};

export default function ProfilePage() {
  const router = useRouter();
  const { isAuthenticated, user, login } = useAuthStore();
  const [gamification, setGamification] = useState<GamificationResponse | null>(null);
  const [editName, setEditName] = useState("");
  const [editNickname, setEditNickname] = useState("");
  const [editingProfile, setEditingProfile] = useState(false);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordMsg, setPasswordMsg] = useState("");
  const [loading, setLoading] = useState(true);
  const [uploadingPic, setUploadingPic] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [prefTypes, setPrefTypes] = useState<Set<string>>(new Set(SPOT_TYPES.map(s => s.value)));
  const [prefTrust, setPrefTrust] = useState<Set<string>>(new Set(TRUST_LEVELS.map(s => s.value)));
  const [prefFreeOnly, setPrefFreeOnly] = useState(false);
  const [prefSaving, setPrefSaving] = useState(false);
  const [prefMsg, setPrefMsg] = useState("");

  useEffect(() => {
    if (!isAuthenticated) { router.push("/login"); return; }
    if (user) {
      setEditName(user.name);
      setEditNickname(user.nickname || "");
    }
    usersApi.getGamification()
      .then((res) => setGamification(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
    usersApi.getPreferences()
      .then((res) => {
        const d = res.data;
        if (d.defaultSpotTypes?.length) setPrefTypes(new Set(d.defaultSpotTypes));
        if (d.defaultTrustLevels?.length) setPrefTrust(new Set(d.defaultTrustLevels));
        setPrefFreeOnly(d.freeOnly || false);
      }).catch(() => {});
  }, [isAuthenticated, user, router]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileError("");
    setProfileLoading(true);
    try {
      await usersApi.updateProfile(editName, editNickname || null);
      // The API returns UserResponse - update auth store
      if (user) {
        const token = localStorage.getItem("token");
        if (token) login(token, { ...user, name: editName, nickname: editNickname || null });
      }
      setEditingProfile(false);
    } catch {
      setProfileError("Falha ao atualizar perfil");
    } finally {
      setProfileLoading(false);
    }
  };

  const handlePicUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingPic(true);
    try {
      const res = await usersApi.updateProfilePic(file);
      if (user) {
        const token = localStorage.getItem("token");
        if (token) login(token, { ...user, profilePicUrl: res.data.profilePicUrl });
      }
    } catch {
      alert("Falha ao enviar foto");
    } finally {
      setUploadingPic(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordMsg("");
    if (newPassword.length < 8) { setPasswordMsg("Senha deve ter pelo menos 8 caracteres"); return; }
    setPasswordLoading(true);
    try {
      await usersApi.changePassword(currentPassword, newPassword);
      setPasswordMsg("Senha alterada com sucesso!");
      setCurrentPassword("");
      setNewPassword("");
    } catch {
      setPasswordMsg("Falha ao alterar senha");
    } finally {
      setPasswordLoading(false);
    }
  };

  if (!isAuthenticated || !user) return null;

  const picUrl = user.profilePicUrl ? `${(process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080")}${user.profilePicUrl}` : null;

  return (
    <div className="mx-auto w-full max-w-2xl px-4 py-6">
      <h1 className="mb-6 text-2xl font-bold text-gray-900">{t("profile.title")}</h1>

      {/* User Info + Avatar */}
      <div className="mb-6 rounded-lg border border-gray-200 bg-white p-6">
        <div className="flex items-start gap-5">
          {/* Profile Pic */}
          <div className="flex flex-col items-center gap-2">
            <button
              onClick={() => fileInputRef.current?.click()}
              className="group relative h-20 w-20 overflow-hidden rounded-full border-2 border-gray-200 hover:border-blue-400"
              title="Alterar foto"
            >
              {picUrl ? (
                <img src={picUrl} alt="" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full items-center justify-center bg-blue-100 text-2xl font-bold text-blue-700">
                  {(user.nickname || user.name || "?").charAt(0).toUpperCase()}
                </div>
              )}
              <div className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition group-hover:opacity-100">
                <svg className="h-6 w-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </div>
            </button>
            <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={handlePicUpload} />
            {uploadingPic && <span className="text-xs text-gray-500">Enviando...</span>}
          </div>

          {/* Name + Nickname */}
          <div className="flex-1">
            {editingProfile ? (
              <form onSubmit={handleUpdateProfile} className="space-y-2">
                <div>
                  <label className="text-xs text-gray-500">Nome</label>
                  <input type="text" value={editName} onChange={(e) => setEditName(e.target.value)} required
                    className="w-full rounded-md border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none" />
                </div>
                <div>
                  <label className="text-xs text-gray-500">Apelido</label>
                  <input type="text" value={editNickname} onChange={(e) => setEditNickname(e.target.value)} maxLength={50}
                    placeholder="Como quer ser chamado?"
                    className="w-full rounded-md border border-gray-300 px-2 py-1 text-sm focus:border-blue-500 focus:outline-none" />
                </div>
                <div className="flex gap-2">
                  <button type="submit" disabled={profileLoading}
                    className="rounded-md bg-blue-600 px-3 py-1 text-xs text-white hover:bg-blue-700 disabled:opacity-50">
                    {t("profile.save")}
                  </button>
                  <button type="button" onClick={() => { setEditingProfile(false); setEditName(user.name); setEditNickname(user.nickname || ""); }}
                    className="text-xs text-gray-500 hover:underline">Cancelar</button>
                </div>
                {profileError && <p className="text-xs text-red-600">{profileError}</p>}
              </form>
            ) : (
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-semibold text-gray-900">{user.name}</h2>
                  <button onClick={() => setEditingProfile(true)} className="text-xs text-blue-600 hover:underline">Editar</button>
                </div>
                {user.nickname && <p className="text-sm text-gray-600">@{user.nickname}</p>}
                <p className="mt-1 text-sm text-gray-500">{user.email}</p>
              </div>
            )}
          </div>

          {/* Reputation */}
          <div className="text-right">
            <p className="text-xs text-gray-500">{t("profile.reputation")}</p>
            <p className="text-2xl font-bold text-blue-600">{user.reputationScore.toFixed(0)}</p>
          </div>
        </div>

        <div className="mt-4 flex gap-3">
          <Link href="/my-spots" className="text-sm text-blue-600 hover:underline">{t("nav.mySpots")}</Link>
          <Link href="/favorites" className="text-sm text-blue-600 hover:underline">{t("nav.favorites")}</Link>
        </div>
      </div>

      {/* Change Password */}
      <div className="mb-6 rounded-lg border border-gray-200 bg-white p-6">
        <h3 className="mb-4 text-lg font-semibold text-gray-900">{t("profile.changePassword")}</h3>
        <form onSubmit={handleChangePassword} className="space-y-3">
          <input type="password" placeholder={t("profile.currentPassword")} required value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" />
          <input type="password" placeholder={t("profile.newPassword")} required value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" />
          <button type="submit" disabled={passwordLoading}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
            {passwordLoading ? "Alterando..." : t("profile.change")}
          </button>
          {passwordMsg && <p className={`text-sm ${passwordMsg.includes("sucesso") ? "text-green-600" : "text-red-600"}`}>{passwordMsg}</p>}
        </form>
      </div>

      {/* Gamification */}
      {/* Preferences */}
      <div className="mb-6 rounded-lg border border-gray-200 bg-white p-6">
        <h3 className="mb-4 text-lg font-semibold text-gray-900">Preferências do Mapa</h3>
        <p className="mb-3 text-sm text-gray-500">Filtros padrão aplicados ao abrir o mapa</p>

        <div className="mb-4">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Tipos de vaga</p>
          <div className="flex flex-wrap gap-2">
            {SPOT_TYPES.map(st => (
              <label key={st.value} className="flex items-center gap-1.5 text-sm">
                <input type="checkbox" className="rounded" checked={prefTypes.has(st.value)}
                  onChange={() => setPrefTypes(prev => { const n = new Set(prev); n.has(st.value) ? n.delete(st.value) : n.add(st.value); return n; })} />
                {st.label}
              </label>
            ))}
          </div>
        </div>

        <div className="mb-4">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">Níveis de confiança</p>
          <div className="flex flex-wrap gap-2">
            {TRUST_LEVELS.map(tl => (
              <label key={tl.value} className="flex items-center gap-1.5 text-sm">
                <input type="checkbox" className="rounded" checked={prefTrust.has(tl.value)}
                  onChange={() => setPrefTrust(prev => { const n = new Set(prev); n.has(tl.value) ? n.delete(tl.value) : n.add(tl.value); return n; })} />
                {tl.label}
              </label>
            ))}
          </div>
        </div>

        <label className="mb-4 flex items-center gap-2 text-sm">
          <input type="checkbox" className="rounded" checked={prefFreeOnly}
            onChange={() => setPrefFreeOnly(!prefFreeOnly)} />
          Somente vagas grátis
        </label>

        <button
          disabled={prefSaving}
          onClick={async () => {
            setPrefSaving(true);
            setPrefMsg("");
            try {
              await usersApi.updatePreferences({
                defaultSpotTypes: Array.from(prefTypes),
                defaultTrustLevels: Array.from(prefTrust),
                freeOnly: prefFreeOnly,
              });
              setPrefMsg("Preferências salvas!");
            } catch { setPrefMsg("Falha ao salvar"); }
            finally { setPrefSaving(false); }
          }}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {prefSaving ? "Salvando..." : "Salvar preferências"}
        </button>
        {prefMsg && <p className={`mt-2 text-sm ${prefMsg.includes("salvas") ? "text-green-600" : "text-red-600"}`}>{prefMsg}</p>}
      </div>

      {/* Gamification */}
      {loading ? (
        <div className="text-center text-sm text-gray-500">{t("common.loading")}</div>
      ) : gamification ? (
        <div className="rounded-lg border border-gray-200 bg-white p-6">
          <h3 className="mb-4 text-lg font-semibold text-gray-900">Gamificação</h3>

          <div className="mb-4 grid grid-cols-3 gap-4">
            <div className="rounded-lg bg-blue-50 p-3 text-center">
              <p className="text-xs text-gray-500">{t("profile.points")}</p>
              <p className="text-xl font-bold text-blue-600">{gamification.totalPoints}</p>
            </div>
            <div className="rounded-lg bg-green-50 p-3 text-center">
              <p className="text-xs text-gray-500">{t("profile.currentStreak")}</p>
              <p className="text-xl font-bold text-green-600">{gamification.streak.currentStreak} {t("profile.days")}</p>
            </div>
            <div className="rounded-lg bg-purple-50 p-3 text-center">
              <p className="text-xs text-gray-500">{t("profile.longestStreak")}</p>
              <p className="text-xl font-bold text-purple-600">{gamification.streak.longestStreak} {t("profile.days")}</p>
            </div>
          </div>

          <h4 className="mb-2 text-sm font-medium text-gray-700">{t("profile.badges")}</h4>
          {gamification.badges.length > 0 ? (
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {gamification.badges.map((badge) => (
                <div key={badge.type} className="flex items-center gap-2 rounded-md bg-yellow-50 px-3 py-2">
                  <span className="text-lg">🏆</span>
                  <div>
                    <p className="text-xs font-medium text-gray-800">{BADGE_LABELS[badge.type] || badge.type}</p>
                    <p className="text-[10px] text-gray-400">{new Date(badge.earnedAt).toLocaleDateString()}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-500">{t("profile.noBadges")}</p>
          )}
        </div>
      ) : null}
    </div>
  );
}
