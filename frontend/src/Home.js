import React, { Component } from 'react';
import './App.css';
import AppNavbar from './AppNavbar';
import { Link } from 'react-router-dom';
import { Button, Container } from 'reactstrap';

class Home extends Component {
  render() {
    return (
      <div>
        <AppNavbar/>
        <Container fluid>
          {/* Rendered as the link itself: a <Link> nested inside a <button> is
              invalid HTML and puts two controls in the tab order for one action. */}
          <Button color="primary" className="mt-3" tag={Link} to="/games/new">Play A Game</Button>
        </Container>
      </div>
    );
  }
}

export default Home;