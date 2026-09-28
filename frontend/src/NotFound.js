import React from 'react';
import { Container } from 'reactstrap';
import { Link } from 'react-router-dom';
import AppNavbar from './AppNavbar';

// Without a catch-all route <Switch> renders null, which left an unknown URL as
// a blank page with no navigation.
const NotFound = () => (
  <div>
    <AppNavbar/>
    <Container className="mt-3">
      <h3>Page not found</h3>
      <p>That page does not exist. <Link to="/">Go back home</Link>.</p>
    </Container>
  </div>
);

export default NotFound;
