import type { NextConfig } from "next";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";
const nextConfig: NextConfig = {
  /* config options here */
    async rewrites() {
        return[
            {
                source: "/api/:path*",
                destination: `${BACKEND_URL}/api/:path*`,
            },
        ];
    },
};

export default nextConfig;
