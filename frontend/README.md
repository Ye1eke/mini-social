# MiniSocial Frontend

This is the Next.js frontend for the MiniSocial MVP application.

## Project Structure

```
frontend/
├── app/              # Next.js App Router pages and layouts
├── components/       # React components
├── lib/              # Utility modules (API client, auth)
├── types/            # TypeScript type definitions
├── public/           # Static assets
└── next.config.ts    # Next.js configuration
```

## Getting Started

1. Install dependencies:

```bash
npm install
```

2. Set up environment variables:

Copy `.env.local.example` to `.env.local` and configure:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
```

3. Run the development server:

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

## Tech Stack

- **Framework**: Next.js 14+ with App Router
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **HTTP Client**: Axios
- **Authentication**: JWT tokens

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm start` - Start production server
- `npm run lint` - Run ESLint

## Configuration

- **next.config.ts**: Configures image domains for Backblaze B2 and environment variables
- **tailwind.config.js**: Tailwind CSS configuration (managed by Next.js 14+)
- **tsconfig.json**: TypeScript configuration
