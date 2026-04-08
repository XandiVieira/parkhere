"use client";

import { useState, useEffect } from "react";
import "@/lib/leaflet-fix";
import { MapContainer, TileLayer, Marker, useMapEvents, useMap } from "react-leaflet";

interface LocationPickerProps {
  onLocationSelect: (lat: number, lng: number) => void;
}

function ClickHandler({ onLocationSelect }: LocationPickerProps) {
  useMapEvents({
    click: (e) => {
      onLocationSelect(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

function FlyToUser() {
  const map = useMap();
  useEffect(() => {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        map.flyTo([pos.coords.latitude, pos.coords.longitude], 15);
      },
      () => {},
      { timeout: 5000 }
    );
  }, [map]);
  return null;
}

export default function LocationPicker({ onLocationSelect }: LocationPickerProps) {
  const [position, setPosition] = useState<[number, number] | null>(null);

  const handleClick = (lat: number, lng: number) => {
    setPosition([lat, lng]);
    onLocationSelect(lat, lng);
  };

  return (
    <MapContainer center={[-30.0346, -51.2177]} zoom={15} className="h-full w-full">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <FlyToUser />
      <ClickHandler onLocationSelect={handleClick} />
      {position && <Marker position={position} />}
    </MapContainer>
  );
}
