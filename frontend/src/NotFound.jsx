import { Link } from 'react-router';
import AppNavbar from './AppNavbar';

// Without a catch-all route <Switch> renders null, which left an unknown URL as
// a blank page with no navigation.
const NotFound = () => (
  <div>
    <AppNavbar/>
    <main className="container mt-3">
      <h1>Page not found</h1>
      <p>That page does not exist. <Link to="/">Go back home</Link>.</p>
    </main>
  </div>
);

export default NotFound;
