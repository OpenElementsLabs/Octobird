import Header from "@/components/header";

export default function Home() {
  return (
    <>
      <Header />
      <main className="mx-auto max-w-4xl p-8">
        <h2 className="font-heading text-2xl font-bold text-oe-dark mb-4">
          Repositories
        </h2>
        <p className="text-oe-mid-gray">Loading repositories...</p>
      </main>
    </>
  );
}
