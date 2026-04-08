"use client";

import { GoogleOAuthProvider, GoogleLogin } from "@react-oauth/google";

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID
  || "345298825311-1vpn0jdmbulhrfnhoor2incsnnlgmfs6.apps.googleusercontent.com";

interface GoogleLoginButtonProps {
  onSuccess: (credential: string) => void;
  onError: () => void;
}

export default function GoogleLoginButton({ onSuccess, onError }: GoogleLoginButtonProps) {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <GoogleLogin
        onSuccess={(response) => {
          if (response.credential) onSuccess(response.credential);
        }}
        onError={onError}
        text="signin_with"
        shape="rectangular"
        width="350"
      />
    </GoogleOAuthProvider>
  );
}
