import React, { Component } from 'react';

// A render-time throw anywhere below here used to unmount the whole tree and
// leave a blank white page with nothing in the UI to explain it.
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.error('Unhandled error in component tree', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="container mt-3">
          <h3>Something went wrong</h3>
          <p>Please reload the page to keep playing.</p>
        </div>
      );
    }
    return this.props.children;
  }
}

export default ErrorBoundary;
