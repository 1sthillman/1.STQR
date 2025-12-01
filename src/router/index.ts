import { useRoutes, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import routes from './config';

const Router = () => {
  const navigate = useNavigate();
  const element = useRoutes(routes);

  useEffect(() => {
    // Set global navigate function
    window.REACT_APP_NAVIGATE = navigate;
  }, [navigate]);

  return element;
};

export default Router;


