import { Component } from 'react';
import { Link } from 'react-router-dom';

const MENU_ID = 'app-navbar-menu';

// Plain Bootstrap 5 markup. reactstrap 8 targeted Bootstrap 4 and emitted
// class names Bootstrap 5 no longer defines (#39).
export default class AppNavbar extends Component {
  constructor(props) {
    super(props);
    this.state = {isOpen: false};
    this.toggle = this.toggle.bind(this);
  }

  toggle() {
    this.setState(previous => ({
      isOpen: !previous.isOpen
    }));
  }

  render() {
    const { isOpen } = this.state;
    return (
      <nav className="navbar navbar-expand-md navbar-dark bg-dark">
        <div className="container-fluid">
          <Link className="navbar-brand" to="/">Home</Link>
          <button className="navbar-toggler" type="button" onClick={this.toggle}
                  aria-controls={MENU_ID} aria-expanded={isOpen} aria-label="Toggle navigation">
            <span className="navbar-toggler-icon"/>
          </button>
          <div className={`collapse navbar-collapse${isOpen ? ' show' : ''}`} id={MENU_ID}>
            <ul className="navbar-nav ms-auto">
              <li className="nav-item">
                <a className="nav-link" href="https://zohebrahiman.com">Zoheb Rahiman</a>
              </li>
              <li className="nav-item">
                <a className="nav-link" href="https://en.wikipedia.org/wiki/Bulls_and_Cows">Cows And Bulls</a>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    );
  }
}
